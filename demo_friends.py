import requests
import json
import time
import sys
import random
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080/api"
TARGET_FRIEND_CODE = "171C49"

def register_user(username, password):
    r = requests.post(f"{BASE_URL}/auth/register", json={"username": username, "password": password})
    if r.status_code != 200:
        print(f"Failed to register {username}", r.text)
        r = requests.post(f"{BASE_URL}/auth/login", json={"username": username, "password": password})
        if r.status_code != 200:
            return None
    return r.json()

def sync_backup(token, workouts, sets, preferences):
    payload = {
        "workouts": workouts,
        "sets": sets,
        "preferences": preferences
    }
    r = requests.post(
        f"{BASE_URL}/sync/backup", 
        json=payload, 
        headers={"Authorization": f"Bearer {token}"}
    )
    return r.status_code == 200

def send_friend_request(token, friend_code):
    r = requests.post(
        f"{BASE_URL}/friends/request?code={friend_code}", 
        headers={"Authorization": f"Bearer {token}"}
    )
    return r.status_code, r.text

def generate_history(base_time, num_workouts, start_id, start_set_id, exercise_configs):
    workouts = []
    sets = []
    
    current_set_id = start_set_id
    current_time = base_time - timedelta(days=num_workouts * 3) # e.g. 1 workout every 3 days
    
    for i in range(num_workouts):
        current_time += timedelta(days=random.randint(2, 4))
        w_id = start_id + i
        workouts.append({
            "id": w_id, 
            "timestamp": int(current_time.timestamp() * 1000), 
            "notes": f"Workout {i+1} - Great pump!"
        })
        
        # Each workout has a few exercises
        for ex_id, ex_prog in exercise_configs.items():
            if random.random() > 0.8: # 80% chance to do this exercise
                continue
            
            # Progress weight over time
            progress_ratio = i / float(num_workouts)
            weight = ex_prog["start_weight"] + (ex_prog["end_weight"] - ex_prog["start_weight"]) * progress_ratio
            weight += random.uniform(-2.5, 5.0) # some variance
            
            for s in range(3): # 3 sets per exercise
                sets.append({
                    "id": current_set_id,
                    "workoutId": w_id,
                    "exerciseId": ex_id,
                    "weight": round(weight * 2.0) / 2.0, # round to nearest 0.5
                    "reps": random.randint(5, 12)
                })
                current_set_id += 1
                
    return workouts, sets, start_id + num_workouts, current_set_id

def main():
    now = datetime.now()
    
    # Generate Arni's history
    arni_w, arni_s, next_w, next_s = generate_history(
        now, 
        20, 
        1, 
        1, 
        {
            1: {"start_weight": 80.0, "end_weight": 140.0}, # Bench Press?
            2: {"start_weight": 100.0, "end_weight": 160.0}, # Squat?
            3: {"start_weight": 40.0, "end_weight": 70.0}   # Curls?
        }
    )
    
    # Generate Ronnie's history
    ronnie_w, ronnie_s, _, _ = generate_history(
        now, 
        30, 
        next_w, 
        next_s, 
        {
            1: {"start_weight": 140.0, "end_weight": 180.0}, 
            2: {"start_weight": 180.0, "end_weight": 220.0}, 
            4: {"start_weight": 100.0, "end_weight": 140.0}
        }
    )
    
    # Generate Jay's history (New user!)
    jay_w, jay_s, _, _ = generate_history(
        now, 
        15, 
        1000, 
        10000, 
        {
            1: {"start_weight": 120.0, "end_weight": 150.0}, 
            3: {"start_weight": 60.0, "end_weight": 80.0}, 
            5: {"start_weight": 80.0, "end_weight": 110.0}
        }
    )

    users_data = [
        {
            "username": "demo_arni_5594",
            "password": "password",
            "workouts": arni_w,
            "sets": arni_s,
            "preferences": {
                "height": 188.0,
                "weight": 110.0,
                "age": 30,
                "language": "en",
                "isOnboardingCompleted": True,
                "lastSeenMuscleRanks": {
                    "group_chest": "ELITE",
                    "group_back": "DIAMOND",
                    "group_legs": "PLATINUM",
                    "group_arms": "ELITE",
                    "group_shoulders": "GOLD",
                    "group_core": "SILVER"
                }
            }
        },
        {
            "username": "demo_ronnie_2650",
            "password": "password",
            "workouts": ronnie_w,
            "sets": ronnie_s,
            "preferences": {
                "height": 180.0,
                "weight": 130.0,
                "age": 35,
                "language": "en",
                "isOnboardingCompleted": True,
                "lastSeenMuscleRanks": {
                    "group_chest": "ELITE",
                    "group_back": "ELITE",
                    "group_legs": "ELITE",
                    "group_arms": "DIAMOND",
                    "group_shoulders": "ELITE",
                    "group_core": "PLATINUM"
                }
            }
        },
        {
            "username": "demo_jay_6109",
            "password": "password",
            "workouts": jay_w,
            "sets": jay_s,
            "preferences": {
                "height": 175.0,
                "weight": 115.0,
                "age": 32,
                "language": "en",
                "isOnboardingCompleted": True,
                "lastSeenMuscleRanks": {
                    "group_chest": "PLATINUM",
                    "group_back": "GOLD",
                    "group_legs": "GOLD",
                    "group_arms": "SILVER",
                    "group_shoulders": "ELITE",
                    "group_core": "BRONZE"
                }
            }
        }
    ]

    for data in users_data:
        print(f"Registering {data['username']}...")
        user_info = register_user(data["username"], data["password"])
        if not user_info:
            print("Failed to register/login")
            continue
        
        token = user_info["token"]
        print(f"Syncing data for {data['username']} ({len(data['workouts'])} workouts, {len(data['sets'])} sets)...")
        success = sync_backup(token, data["workouts"], data["sets"], data["preferences"])
        print(f"Sync {'successful' if success else 'failed'}")
        
        print(f"Sending friend request to {TARGET_FRIEND_CODE}...")
        status, response_text = send_friend_request(token, TARGET_FRIEND_CODE)
        print(f"Friend request status: {status}, response: {response_text}\n")

if __name__ == "__main__":
    main()
