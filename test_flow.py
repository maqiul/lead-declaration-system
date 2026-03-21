import requests
import json
import time

base_url = "http://localhost:8080/api/v1"

# 1. Login to get token
print("Logging in...")
login_res = requests.post(
    f"{base_url}/users/login",
    json={"username": "admin", "password": "123"}
)
if login_res.status_code == 200 and login_res.json().get('code') == 200:
    token_value = login_res.json()['data']['tokenValue']
    token_name = login_res.json()['data']['tokenName']
    headers = {token_name: token_value, "Content-Type": "application/json"}
    print("Login successful.")
else:
    print(f"Login failed: {login_res.text}")
    exit(1)

# 2. Create a new declaration form
print("Creating declaration form...")
form_data = {
    "shipperCompany": "Test Company",
    "consigneeCompany": "Receiver Inc",
    "totalAmount": 1000
}
create_res = requests.post(f"{base_url}/declarations", json=form_data, headers=headers)
print(f"Create response: {create_res.text}")

if create_res.status_code == 200 and create_res.json().get('code') == 200:
    form_id = create_res.json()['data']
    print(f"Created form with ID: {form_id}")
    
    # 3. Submit the form to trigger flowable process start
    print(f"Submitting form {form_id} to start process...")
    submit_res = requests.post(f"{base_url}/declarations/{form_id}/submit", headers=headers)
    print(f"Submit response: {submit_res.text}")
    
    # 4. Try to audit it 
    print(f"Attempting to audit form {form_id}...")
    audit_res = requests.post(
        f"{base_url}/declarations/{form_id}/audit", 
        json={"result": "1", "comment": "testing"}, 
        headers=headers
    )
    print(f"Audit response: {audit_res.text}")
else:
    print("Creation failed, stopping.")
