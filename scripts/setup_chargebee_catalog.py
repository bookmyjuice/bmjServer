from chargebee import Chargebee

# Configuration for bookmyjuice-test site
CHARGEBEE_API_KEY = "test_fMwLpyDFENxTWox6zsbpaYNAoL3yiY9v"
CHARGEBEE_SITE = "bookmyjuice-test"

# Initialize Client for SDK v3
cb = Chargebee(api_key=CHARGEBEE_API_KEY, site=CHARGEBEE_SITE)

CATEGORIES = ["Delight", "Signature", "Premium"]
FLAVORS = ["Orange", "Apple", "Pineapple"]
SIZES = [
    {"id": "200ml", "otp": 7500, "weekly": 40000, "monthly": 150000},
    {"id": "300ml", "otp": 12900, "weekly": 65000, "monthly": 240000},
    {"id": "500ml", "otp": 18000, "weekly": 90000, "monthly": 330000}
]

def setup_catalog():
    print(f"🚀 Provisioning Chargebee Catalog for site: {CHARGEBEE_SITE}")
    
    for cat in CATEGORIES:
        for flav in FLAVORS:
            meta = f'{{"category":"{cat}", "image_url":"https://cdn.bookmyjuice.co.in/{cat.lower()}_{flav.lower()}.jpg", "flavor":"{flav}"}}'
            
            item_id = f"charge_{cat.lower()}_{flav.lower()}"
            
            try:
                # 1. Create Item (Type: Charge for one-time)
                cb.Item.create({
                    "id": item_id,
                    "name": f"{flav} Juice ({cat})",
                    "description": f"Fresh {flav} juice from our {cat} category.",
                    "type": "charge",
                    "status": "active",
                    "meta_data": meta
                })
                print(f"✅ Created Item: {item_id}")
                
                # 2. Create Plans (Weekly & Monthly)
                for freq, price_key in [("weekly", "weekly"), ("monthly", "monthly")]:
                    plan_id = f"plan_{cat.lower()}_{flav.lower()}_{freq}"
                    try:
                        cb.Plan.create({
                            "id": plan_id,
                            "item_id": item_id,
                            "name": f"{flav} Juice - {freq.capitalize()}",
                            "status": "active",
                            "meta_data": meta
                        })
                        print(f"   📅 Created Plan: {plan_id}")
                    except Exception as e: 
                        print(f"   ⚠️ Plan {plan_id} exists: {e}")

                # 3. Create ItemPrices for each size (200ml, 300ml, 500ml)
                for s in SIZES:
                    try:
                        cb.ItemPrice.create({
                            "item_id": item_id,
                            "price": s["otp"],
                            "period_unit": "day",
                            "pricing_model": "flat_fee",
                            "name": f"{s['id']} One-Time"
                        })
                        
                        cb.ItemPrice.create({
                            "item_id": item_id,
                            "price": s["weekly"],
                            "period_unit": "week",
                            "period": 1,
                            "pricing_model": "flat_fee",
                            "name": f"{s['id']} Weekly"
                        })

                        cb.ItemPrice.create({
                            "item_id": item_id,
                            "price": s["monthly"],
                            "period_unit": "month",
                            "period": 1,
                            "pricing_model": "flat_fee",
                            "name": f"{s['id']} Monthly"
                        })
                    except Exception as e:
                        print(f"   💰 Price exists for {item_id}/{s['id']}: {e}")
                        
                print(f"✅ Processed {flav} ({cat})")
                
            except Exception as e: 
                print(f"⚠️ Item {item_id} exists: {e}")

    print("✅ Chargebee Catalog Provisioning Complete!")

if __name__ == "__main__":
    setup_catalog()
