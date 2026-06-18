use anyhow::{Context, Result};
use chrono::Local;
use clap::Parser;
use reqwest::blocking::Client;
use reqwest::header::CONTENT_TYPE;
use serde::{Deserialize, Serialize};

#[derive(Parser, Debug)]
#[command(name = "pi-trmnl", version = "1.0.0", about = "Fetches PADD data from a Pi-hole server and publishes it to a TRMNL plugin.")]
struct Args {
    #[arg(short = 'e', long = "pihole-endpoint", help = "The endpoint of your Pi-hole server")]
    pihole_endpoint: String,

    #[arg(short = 'p', long = "pihole-password", help = "The application password of your Pi-hole server")]
    pihole_password: String,

    #[arg(short = 't', long = "trmnl-plugin", help = "The plugin UUID of your TRMNL plugin")]
    trmnl_plugin: String,
}

#[derive(Deserialize)]
struct AuthResponse {
    session: Session,
}

#[derive(Deserialize)]
struct Session {
    sid: String,
    csrf: String,
}

#[derive(Deserialize)]
struct PaddData {
    system: SystemData,
    sensors: SensorsData,
    blocking: String,
    #[serde(rename = "node_name")]
    node_name: String,
    version: VersionData,
}

#[derive(Deserialize)]
struct SystemData {
    cpu: CpuData,
    memory: MemoryData,
}

#[derive(Deserialize)]
struct CpuData {
    #[serde(rename = "%cpu")]
    percent_cpu: f64,
    load: LoadData,
}

#[derive(Deserialize)]
struct LoadData {
    percent: Vec<f64>,
}

#[derive(Deserialize)]
struct MemoryData {
    ram: RamData,
}

#[derive(Deserialize)]
struct RamData {
    #[serde(rename = "%used")]
    percent_used: f64,
}

#[derive(Deserialize)]
struct SensorsData {
    #[serde(rename = "cpu_temp")]
    cpu_temp: f64,
    unit: String,
    #[serde(rename = "hot_limit")]
    hot_limit: f64,
}

#[derive(Deserialize)]
struct VersionData {
    core: ComponentVersion,
    web: ComponentVersion,
    ftl: ComponentVersion,
}

#[derive(Deserialize)]
struct ComponentVersion {
    local: VersionInfo,
    remote: VersionInfo,
}

#[derive(Deserialize)]
struct VersionInfo {
    version: String,
}

#[derive(Deserialize)]
struct HistoryData {
    history: Vec<HistoryEntry>,
}

#[derive(Deserialize)]
struct HistoryEntry {
    timestamp: i64,
    total: i32,
    blocked: i32,
}

#[derive(Serialize)]
struct ScreenVariables {
    #[serde(rename = "cpu_percent")]
    cpu_percent: String,
    #[serde(rename = "cpu_load")]
    cpu_load: Vec<String>,
    #[serde(rename = "cpu_temp")]
    cpu_temp: String,
    #[serde(rename = "cpu_unit")]
    cpu_unit: String,
    #[serde(rename = "cpu_limit")]
    cpu_limit: String,
    #[serde(rename = "memory_usage")]
    memory_usage: String,
    blocking: String,
    #[serde(rename = "node_name")]
    node_name: String,
    update: bool,
    #[serde(rename = "last_refreshed")]
    last_refreshed: String,
    #[serde(rename = "query_total")]
    query_total: Vec<i32>,
    #[serde(rename = "query_blocked")]
    query_blocked: Vec<i32>,
    #[serde(rename = "query_date")]
    query_date: Vec<i64>,
}

impl PaddData {
    fn cpu_percent(&self) -> String {
        round_to_2_decimal(self.system.cpu.percent_cpu)
    }

    fn cpu_load(&self) -> Vec<String> {
        self.system
            .cpu
            .load
            .percent
            .iter()
            .map(|value| round_to_2_decimal(*value))
            .collect()
    }

    fn cpu_temp(&self) -> String {
        round_to_2_decimal(self.sensors.cpu_temp)
    }

    fn cpu_unit(&self) -> &str {
        &self.sensors.unit
    }

    fn cpu_limit(&self) -> String {
        round_to_2_decimal(self.sensors.hot_limit)
    }

    fn memory_percent(&self) -> String {
        round_to_2_decimal(self.system.memory.ram.percent_used)
    }

    fn update_required(&self) -> bool {
        self.version.core.local.version != self.version.core.remote.version
            || self.version.web.local.version != self.version.web.remote.version
            || self.version.ftl.local.version != self.version.ftl.remote.version
    }
}

impl HistoryData {
    fn limited_history(&self) -> &[HistoryEntry] {
        let start = self.history.len().saturating_sub(80);
        &self.history[start..]
    }

    fn total_queries(&self) -> Vec<i32> {
        self.limited_history().iter().map(|entry| entry.total).collect()
    }

    fn blocked_queries(&self) -> Vec<i32> {
        self.limited_history().iter().map(|entry| entry.blocked).collect()
    }

    fn timestamp_queries(&self) -> Vec<i64> {
        self.limited_history()
            .iter()
            .map(|entry| entry.timestamp * 1000)
            .collect()
    }
}

impl ScreenVariables {
    fn new(padd: &PaddData, history: &HistoryData) -> Self {
        let now = Local::now().format("%Y-%m-%d %H:%M").to_string();
        Self {
            cpu_percent: padd.cpu_percent(),
            cpu_load: padd.cpu_load(),
            cpu_temp: padd.cpu_temp(),
            cpu_unit: padd.cpu_unit().to_string(),
            cpu_limit: padd.cpu_limit(),
            memory_usage: padd.memory_percent(),
            blocking: padd.blocking.clone(),
            node_name: padd.node_name.clone(),
            update: padd.update_required(),
            last_refreshed: now,
            query_total: history.total_queries(),
            query_blocked: history.blocked_queries(),
            query_date: history.timestamp_queries(),
        }
    }
}

fn round_to_2_decimal(value: f64) -> String {
    format!("{value:.2}")
}

fn build_client() -> Result<Client> {
    Client::builder()
        .danger_accept_invalid_certs(true)
        .timeout(std::time::Duration::from_secs(10))
        .build()
        .context("Failed to build HTTP client")
}

fn authenticate(client: &Client, endpoint: &str, password: &str) -> Result<Session> {
    let url = format!("https://{endpoint}/api/auth");
    let response = client
        .post(&url)
        .header(CONTENT_TYPE, "application/json")
        .body(serde_json::json!({ "password": password }).to_string())
        .send()
        .context("Failed to send authentication request")?;

    if !response.status().is_success() {
        let status = response.status();
        let body = response.text().unwrap_or_default();
        anyhow::bail!("Failed to authenticate: {status} - {body}");
    }

    let body = response
        .text()
        .context("Failed to read authentication response body")?;
    let auth: AuthResponse = serde_json::from_str(&body)
        .context("Failed to parse authentication response")?;
    Ok(auth.session)
}

fn fetch_padd_data(client: &Client, endpoint: &str, session: &Session) -> Result<PaddData> {
    println!("⬇ Fetching PADD data from Pi-hole server...");
    let url = format!("https://{endpoint}/api/padd?full=true");
    let response = client
        .get(&url)
        .header("X-FTL-SID", &session.sid)
        .header("X-FTL-CSRF", &session.csrf)
        .send()
        .context("Failed to send PADD request")?;

    if !response.status().is_success() {
        let status = response.status();
        let body = response.text().unwrap_or_default();
        anyhow::bail!("Failed to fetch PADD data: {status} - {body}");
    }

    let body = response
        .text()
        .context("Failed to read PADD data response body")?;
    serde_json::from_str(&body)
        .context("Failed to parse PADD data response")
}

fn fetch_history(client: &Client, endpoint: &str, session: &Session) -> Result<HistoryData> {
    println!("⬇ Fetching History data from Pi-hole server...");
    let url = format!("https://{endpoint}/api/history");
    let response = client
        .get(&url)
        .header("X-FTL-SID", &session.sid)
        .header("X-FTL-CSRF", &session.csrf)
        .header("Accept", "application/json")
        .send()
        .context("Failed to send History request")?;

    if !response.status().is_success() {
        let status = response.status();
        let body = response.text().unwrap_or_default();
        anyhow::bail!("Failed to fetch History data: {status} - {body}");
    }

    let body = response
        .text()
        .context("Failed to read History data response body")?;
    serde_json::from_str(&body)
        .context("Failed to parse History data response")
}

fn send_to_trmnl(client: &Client, plugin_id: &str, variables: &ScreenVariables) -> Result<()> {
    println!("📤 Sending data to TRMNL plugin...");
    let url = format!("https://usetrmnl.com/api/custom_plugins/{plugin_id}");
    let payload = serde_json::json!({ "merge_variables": variables });

    let response = client
        .post(&url)
        .header(CONTENT_TYPE, "application/json")
        .body(payload.to_string())
        .send()
        .context("Failed to send TRMNL request")?;

    if response.status().as_u16() == 200 {
        println!("Data sent successfully.");
        Ok(())
    } else {
        let status = response.status();
        let body = response.text().unwrap_or_default();
        anyhow::bail!("Failed to send data: {status} - {body}");
    }
}

fn main() {
    let args = Args::parse();
    println!("★ Pi-Trmnl");

    match run(&args) {
        Ok(()) => std::process::exit(0),
        Err(err) => {
            eprintln!("Error in execution: {err}");
            std::process::exit(1);
        }
    }
}

fn run(args: &Args) -> Result<()> {
    let client = build_client()?;
    let session = authenticate(&client, &args.pihole_endpoint, &args.pihole_password)?;
    let padd_data = fetch_padd_data(&client, &args.pihole_endpoint, &session)?;
    let history_data = fetch_history(&client, &args.pihole_endpoint, &session)?;
    let screen_variables = ScreenVariables::new(&padd_data, &history_data);
    send_to_trmnl(&client, &args.trmnl_plugin, &screen_variables)
}
