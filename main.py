from flask import Flask, request, jsonify
import requests
import jwt  # pip install pyjwt[crypto]
import time

app = Flask(__name__)

# ==============================================================================
# CONFIGURATION
# ==============================================================================
# 1. Get these from Samsung Seller Portal > Assistance > API Service > Create Service Account
# See: https://developer.samsung.com/sdp/blog/en/2022/03/29/how-to-create-an-access-token-for-the-galaxy-store-developer-api-using-python
SERVICE_ACCOUNT_ID = "YOUR_SERVICE_ACCOUNT_ID_HERE" 

# 2. Download the key file and paste the content here (keep the -----BEGIN/END lines)
PRIVATE_KEY = """-----BEGIN RSA PRIVATE KEY-----
YOUR_PRIVATE_KEY_CONTENT_HERE
...
-----END RSA PRIVATE KEY-----"""

# 3. Your Android App Package Name
PACKAGE_NAME = "com.notes.keepnotes"

# APIs
SAMSUNG_AUTH_URL = "https://devapi.samsungapps.com/auth/accessToken"
# Subscription Status API (Better than simple receipt verification for subs)
SAMSUNG_SUB_URL_TEMPLATE = "https://devapi.samsungapps.com/iap/seller/v6/applications/{}/purchases/subscriptions/{}"

def get_access_token():
    """
    Generates a JWT and exchanges it for a Samsung Access Token.
    """
    try:
        iat = int(time.time())
        exp = iat + 1200 # Expires in 20 minutes
        
        payload = {
            "iss": SERVICE_ACCOUNT_ID,
            "scopes": ["gss"],
            "iat": iat,
            "exp": exp
        }
        
        # Sign the JWT
        # Note: requires 'cryptography' package installed
        signed_jwt = jwt.encode(payload, PRIVATE_KEY, algorithm='RS256')
        
        # Exchange for Access Token
        headers = {
            'content-type': 'application/json',
            'Authorization': f"Bearer {signed_jwt}"
        }
        
        response = requests.post(SAMSUNG_AUTH_URL, headers=headers, timeout=15)
        
        if response.status_code == 200:
            return response.json()['createdItem']['accessToken']
        else:
            print(f"Auth Error: {response.text}")
            return None
            
    except Exception as e:
        print(f"Token Generation Error: {str(e)}")
        return None

@app.route('/verify_receipt', methods=['GET'])
def verify_receipt():
    """
    Endpoint to verify Samsung Subscription Status.
    Usage: GET /verify_receipt?purchaseID=<PURCHASE_ID>
    """
    purchase_id = request.args.get('purchaseID')
    
    if not purchase_id:
        return jsonify({"status": "fail", "message": "Missing purchaseID"}), 400

    # 1. Get Authentication Token
    access_token = get_access_token()
    if not access_token:
        return jsonify({"status": "error", "message": "Failed to authenticate with Samsung Server. Check Service Account ID and Key."}), 500

    try:
        # 2. Call Samsung Subscription Status API
        url = SAMSUNG_SUB_URL_TEMPLATE.format(PACKAGE_NAME, purchase_id)
        
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {access_token}",
            "service-account-id": SERVICE_ACCOUNT_ID
        }
        
        response = requests.get(url, headers=headers, timeout=15)
        
        # 3. Process Response
        if response.status_code == 200:
            data = response.json()
            # The structure of success response:
            # {
            #   "subscriptionStatus": "ACTIVE" | "CANCEL" | ...,
            #   "subscriptionEndDate": "...",
            #   ...
            # }
            
            # Simple check for active status (adjust logic as needed)
            status = data.get('subscriptionStatus')
            # Consider ACTIVE, GRACE_PERIOD as valid
            # Statuses: ACTIVE, CANCEL, ON_HOLD, GRACE_PERIOD, PAUSED, REVOKED, EXPIRED
            is_valid = status in ['ACTIVE', 'GRACE_PERIOD']
            
            return jsonify({
                "status": "success",
                "is_valid": is_valid,
                "samsung_data": data
            })
        else:
            return jsonify({
                "status": "fail",
                "code": response.status_code,
                "samsung_response": response.json() if response.content else response.text
            }), response.status_code

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    print("Starting server...")
    print("Make sure you have installed requirements:")
    print("pip install flask requests pyjwt[crypto]")
    app.run(host='0.0.0.0', port=5000, debug=True)
