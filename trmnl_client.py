import requests
import json

class TrmnlClient:
    def __init__(self, plugin_id: str):
        self.plugin_id = plugin_id
        self.client = requests.Session()


    def send_data(self, data: dict):
        print("📤 Sending data to TRMNL plugin...")

        url = f"https://usetrmnl.com/api/custom_plugins/{self.plugin_id}"
        payload = {
            "merge_variables": json.dumps(data, separators=(',', ':'))
        }

        print(f"Payload to send: {payload}")

        try:
            response = self.client.post(url, headers={"Content-Type": "application/json"}, json=payload)
            if response.status_code == 200:
                print("Data sent successfully.")
                print(f"Response: {response.text}")
            else:
                print(f"Failed to send data: {response.status_code} - {response.reason}")
                print(f"Error: {response.text}")
        except requests.RequestException as e:
            print(f"Error sending data: {e}")

