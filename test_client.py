import requests
import json
import time
import sys

BASE_URL = "http://localhost:8080/api"

def print_step(step):
    print(f"\n--- {step} ---")

def run():
    # Wait for server to start
    for i in range(30):
        try:
            requests.get("http://localhost:8080")
            break
        except requests.exceptions.ConnectionError:
            time.sleep(1)
    
    print_step("1. Registering User A (Alice)")
    r = requests.post(f"{BASE_URL}/auth/register", json={"username": "alice", "password": "password"})
    if r.status_code != 200:
        print("Failed to register Alice", r.text)
        sys.exit(1)
    
    alice_data = r.json()
    alice_token = alice_data["token"]
    alice_code = alice_data["friendCode"]
    print("Alice registered. Code:", alice_code)

    print_step("2. Registering User B (Bob)")
    r = requests.post(f"{BASE_URL}/auth/register", json={"username": "bob", "password": "password"})
    bob_data = r.json()
    bob_token = bob_data["token"]
    bob_code = bob_data["friendCode"]
    print("Bob registered. Code:", bob_code)

    print_step("3. Alice pushes backup data")
    backup_payload = {
        "workouts": [
            {"id": 1, "timestamp": 1690000000, "notes": "Leg Day"}
        ],
        "sets": [
            {"id": 1, "workoutId": 1, "exerciseId": 10, "weight": 100.0, "reps": 10}
        ],
        "preferences": {
            "height": 180.0,
            "weight": 80.0,
            "age": 25,
            "language": "en",
            "isOnboardingCompleted": True,
            "lastSeenMuscleRanks": {
                "m_legs": "ELITE",
                "m_chest_mid": "GOLD"
            }
        }
    }
    r = requests.post(
        f"{BASE_URL}/sync/backup", 
        json=backup_payload, 
        headers={"Authorization": f"Bearer {alice_token}"}
    )
    print("Alice backup sync status:", r.status_code)
    print("Response:", r.text)

    print_step("4. Alice sends friend request to Bob")
    r = requests.post(
        f"{BASE_URL}/friends/request?code={bob_code}", 
        headers={"Authorization": f"Bearer {alice_token}"}
    )
    print("Friend request status:", r.status_code, r.text)

    print_step("5. Bob gets friend requests")
    r = requests.get(
        f"{BASE_URL}/friends/requests", 
        headers={"Authorization": f"Bearer {bob_token}"}
    )
    requests_data = r.json()
    print("Bob's requests:", json.dumps(requests_data, indent=2))
    
    friendship_id = requests_data[0]["friendshipId"]

    print_step("6. Bob accepts friend request")
    r = requests.post(
        f"{BASE_URL}/friends/accept?friendshipId={friendship_id}", 
        headers={"Authorization": f"Bearer {bob_token}"}
    )
    print("Accept status:", r.status_code, r.text)

    print_step("7. Bob checks leaderboard")
    r = requests.get(
        f"{BASE_URL}/friends/leaderboard", 
        headers={"Authorization": f"Bearer {bob_token}"}
    )
    leaderboard_data = r.json()
    print("Leaderboard Data:\n", json.dumps(leaderboard_data, indent=2))
    
if __name__ == "__main__":
    run()
