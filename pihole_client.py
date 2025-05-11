import requests
from requests.exceptions import RequestException


class Session:
    def __init__(self, sid: str, csrf: str):
        self.sid = sid
        self.csrf = csrf


class PiHoleClient:
    def __init__(self, endpoint: str, password: str):
        self.endpoint = endpoint
        self.password = password
        self.session = None
        self.client = requests.Session()
        self.authenticate()

    def authenticate(self):
        url = f"https://{self.endpoint}/api/auth"
        payload = {"password": self.password}

        try:
            response = self.client.post(url, json=payload, verify=False)
            response.raise_for_status()
            auth_response = response.json()
            self.session = Session(
                sid=auth_response["session"]["sid"],
                csrf=auth_response["session"]["csrf"]
            )
        except RequestException as e:
            raise Exception(f"Error cannot authenticate to PiHole: {e}")
        except KeyError as e:
            raise Exception(f"Failed to parse authentication response: {e}")

    def get_padd_data(self):
        print("⬇ Fetching PADD data from Pi-hole server...")

        if not self.session:
            print("You must authenticate first.")
            return None

        url = f"https://{self.endpoint}/api/padd?full=true"
        headers = {
            "X-FTL-SID": self.session.sid,
            "X-FTL-CSRF": self.session.csrf
        }

        try:
            response = self.client.get(url, headers=headers, verify=False)
            response.raise_for_status()
            return response.text
        except RequestException as e:
            print(f"Failed to fetch PADD data: {e}")
            return None

