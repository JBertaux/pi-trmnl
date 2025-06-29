import argparse
import json
from datetime import datetime
from pihole_client import PiHoleClient
from trmnl_client import TrmnlClient
import urllib3
import os
import csv


urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def round_to_2_decimal(value):
    return f"{value:.2f}"


def seconds_to_human_readable(seconds):
    years = seconds // 31_536_000  # 1 year = 365 days
    months = (seconds % 31_536_000) // 2_592_000  # 1 month = 30 days
    days = (seconds % 2_592_000) // 86_400  # 1 day = 86400 seconds
    hours = (seconds % 86_400) // 3_600  # 1 hour = 3600 seconds
    minutes = (seconds % 3_600) // 60  # 1 minute = 60 seconds
    seconds = seconds % 60

    result = []
    if years > 0:
        result.append(f"{years} years")
    if months > 0 or result:
        result.append(f"{months} months")
    if days > 0 or result:
        result.append(f"{days} days")
    if hours > 0 or result:
        result.append(f"{hours} hours")
    if minutes > 0 or result:
        result.append(f"{minutes} minutes")
    if seconds > 0 or not result:
        result.append(f"{seconds} seconds")

    return ", ".join(result)


def process_padd(data):
    print("⚙️ Convert PADD data...")
    json_data = json.loads(data)

    data_output = {}
    data_output["cpu_percent"] = round_to_2_decimal(json_data["system"]["cpu"]["%cpu"])
    data_output["cpu_temp"] = round_to_2_decimal(json_data["sensors"]["cpu_temp"])
    data_output["cpu_unit"] = json_data["sensors"]["unit"]
    data_output["cpu_limit"] = round_to_2_decimal(json_data["sensors"]["hot_limit"])
    data_output["memory_usage"] = round_to_2_decimal(json_data["system"]["memory"]["ram"]["%used"])
    data_output["uptime"] = seconds_to_human_readable(json_data["system"]["uptime"])
    data_output["blocking"] = json_data["blocking"]
    data_output["query_percent_blocked"] = round_to_2_decimal(json_data["queries"]["percent_blocked"])
    data_output["node_name"] = json_data["node_name"]
    data_output["host_model"] = json_data["host_model"]

    core_local = json_data["version"]["core"]["local"]["version"]
    data_output["version_core_installed"] = core_local

    core_remote = json_data["version"]["core"]["remote"]["version"]
    data_output["version_core_remote"] = core_remote

    data_output["version_core_update"] = str(core_remote != core_local)

    web_local = json_data["version"]["web"]["local"]["version"]
    data_output["version_web_installed"] = web_local

    web_remote = json_data["version"]["web"]["remote"]["version"]
    data_output["version_web_remote"] = web_remote

    data_output["version_web_update"] = str(web_remote != web_local)

    ftl_local = json_data["version"]["ftl"]["local"]["version"]
    data_output["version_ftl_local"] = ftl_local

    ftl_remote = json_data["version"]["ftl"]["remote"]["version"]
    data_output["version_ftl_remote"] = ftl_remote

    data_output["version_ftl_update"] = str(ftl_remote != ftl_local)

    now = datetime.now()
    data_output["last_refreshed"] = now.strftime("%Y-%m-%d %H:%M")

    print("📊 PADD data converted successfully.")
    for key, value in data_output.items():
        print(f"\t{key}: {value}")

    return data_output


def persist_metrics_to_csv(data):
    fields = ["cpu_percent", "memory_usage", "query_percent_blocked", "last_refreshed"]
    row = [data[field] for field in fields]

    # Read existing rows if file exists
    rows = []
    if os.path.exists("pihole-metrics.csv"):
        with open("pihole-metrics.csv", "r", newline="") as csvfile:
            reader = csv.reader(csvfile)
            rows = list(reader)

    # Append new row and keep only last X
    rows.append(row)
    rows = rows[-50:]

    # Write back to file
    with open("pihole-metrics.csv", "w", newline="") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerows(rows)
    
    print("💾 Persisted historic data successfully.")
    
    columns_map = {}
    for i, field in enumerate(fields):
        col = [row[i] for row in rows]
        if field != "last_refreshed":
            # Cast to float
            col = [float(v) for v in col]
        columns_map[f"hist_{field}"] = col
    
    merged_data = {**data, **columns_map} 

    print("📊 Added historic data successfully.")
    print(merged_data)
    return merged_data


def main():
    parser = argparse.ArgumentParser(description="Fetches PADD data from a Pi-hole server and publishes it to a TRMNL plugin.")
    parser.add_argument("-e", "--pihole-endpoint", required=True, help="The endpoint of your Pi-hole server")
    parser.add_argument("-p", "--pihole-password", required=True, help="The application password of your Pi-hole server")
    parser.add_argument("-t", "--trmnl-plugin", required=True, help="The plugin UUID of your TRMNL plugin")
    args = parser.parse_args()

    print("Pi-Trmnl")

    # Placeholder for PiHoleClient and TrmnlClient
    client = PiHoleClient(args.pihole_endpoint, args.pihole_password)
    trmnl_client = TrmnlClient(args.trmnl_plugin)

    data = client.get_padd_data()
    if data:
        processed_data = process_padd(data)
        history_data = persist_metrics_to_csv(processed_data)
        trmnl_client.send_data(history_data)
    else:
        print("Failed to retrieve PADD data.")


if __name__ == "__main__":
    main()

