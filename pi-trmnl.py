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


def process_padd(data):
    print("⚙️ Convert PADD data...")
    json_data = json.loads(data)

    data_output = {}
    data_output["cpu_percent"] = round_to_2_decimal(json_data["system"]["cpu"]["%cpu"])
    data_output["cpu_temp"] = round_to_2_decimal(json_data["sensors"]["cpu_temp"])
    data_output["cpu_unit"] = json_data["sensors"]["unit"]
    data_output["cpu_limit"] = round_to_2_decimal(json_data["sensors"]["hot_limit"])
    data_output["memory_usage"] = round_to_2_decimal(json_data["system"]["memory"]["ram"]["%used"])
    data_output["blocking"] = json_data["blocking"]
    data_output["node_name"] = json_data["node_name"]

    core_local = json_data["version"]["core"]["local"]["version"]
    core_remote = json_data["version"]["core"]["remote"]["version"]
    core_update = str(core_remote != core_local)

    web_local = json_data["version"]["web"]["local"]["version"]
    web_remote = json_data["version"]["web"]["remote"]["version"]
    web_update = str(web_remote != web_local)

    ftl_local = json_data["version"]["ftl"]["local"]["version"]
    ftl_remote = json_data["version"]["ftl"]["remote"]["version"]
    ftl_update = str(ftl_remote != ftl_local)

    data_output["update"] = str(core_update or web_update or ftl_update)

    now = datetime.now()
    data_output["last_refreshed"] = now.strftime("%Y-%m-%d %H:%M")

    print("📊 PADD data converted successfully.")

    return data_output


def process_history(json_history):
    print("⚙️ Convert History data...")

    history_entries = json_history.get("history", [])
    last_entries = history_entries[-40:]

    data_output = {}
    data_output["query_total"] = [entry["total"] for entry in last_entries]
    data_output["query_blocked"] = [entry["blocked"] for entry in last_entries]
    data_output["query_date"] = [entry["timestamp"] * 1000 for entry in last_entries]

    print("📊 History data converted successfully.")

    return data_output


def persist_metrics_to_csv(data):
    fields = ["cpu_percent", "memory_usage", "last_refreshed"]
    row = [data[field] for field in fields]

    # Read existing rows if file exists
    rows = []
    if os.path.exists("pihole-metrics.csv"):
        with open("pihole-metrics.csv", "r", newline="") as csvfile:
            reader = csv.reader(csvfile)
            rows = list(reader)

    # Append new row and keep only last X
    rows.append(row)
    rows = rows[-35:]

    # Write back to file
    with open("pihole-metrics.csv", "w", newline="") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerows(rows)
    
    print("💾 Persisted PADD data successfully.")
    
    columns_map = {}
    for i, field in enumerate(fields):
        col = [row[i] for row in rows]
        if field != "last_refreshed":
            # Cast to float
            col = [float(v) for v in col]
        else:
            col = [int(datetime.strptime(v, "%Y-%m-%d %H:%M").timestamp()) * 1000 for v in col]
        columns_map[f"hist_{field}"] = col
    
    merged_data = {**data, **columns_map} 

    fields_to_remove = fields[:-1]

    for key in fields_to_remove:
        merged_data.pop(key)

    print("📊 Added PADD metrics successfully.")
    return merged_data


def main():
    parser = argparse.ArgumentParser(description="Fetches PADD data from a Pi-hole server and publishes it to a TRMNL plugin.")
    parser.add_argument("-e", "--pihole-endpoint", required=True, help="The endpoint of your Pi-hole server")
    parser.add_argument("-p", "--pihole-password", required=True, help="The application password of your Pi-hole server")
    parser.add_argument("-t", "--trmnl-plugin", required=True, help="The plugin UUID of your TRMNL plugin")
    args = parser.parse_args()

    print("★ Pi-Trmnl")

    client = PiHoleClient(args.pihole_endpoint, args.pihole_password)
    trmnl_client = TrmnlClient(args.trmnl_plugin)

    processed_data = process_padd(client.get_padd_data())
    metrics_data = persist_metrics_to_csv(processed_data)

    history_data = process_history(client.get_history())

    data_to_send = {**metrics_data, **history_data} 

    trmnl_client.send_data(data_to_send)


if __name__ == "__main__":
    main()

