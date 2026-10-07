# pi-trmnl

![PiTrmnl Screen](examples/example.png?raw=true "PiTrmnl Screen")

Rust script to push your Pi-Hole stats to a custom TRMNL plugin

## How to run it ?

### Prerequisite

On raspberry

```bash
sudo apt install libssl-dev
```

### Build it

```bash
cargo build --release
```

### Edit your crontab file

```bash
crontab -e
```

### Cron configuration

```txt
*/30 * * * * /home/pi/rust/target/release/pi-trmnl -e <PIHOLE-ENDPOINT> -p <PIHOLE-PASSWORD> -t <TRMNL-PLUGIN-ID> > ~/crontab_log.txt
```