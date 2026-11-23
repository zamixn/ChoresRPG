package com.zsoftware.choresrpg.choreicon;

import com.zsoftware.choresrpg.R;

import java.util.Locale;

// Icons a chore can have, each with keyword tags that the icon picker's search looks through.
// The constant's name is what gets saved, so existing names must not change.
// Material Design icons from react-icons (Apache License 2.0), as vector drawables named chore_icon_*.
// (The app's first four picture icons were removed; see FromSavedName for chores that still use them.)
public enum ChoreIconEnum
{
    AcUnit(R.drawable.chore_icon_ac_unit, "ac unit air conditioning cooling cold snow filter"),
    AccountBalanceWallet(R.drawable.chore_icon_account_balance_wallet, "account balance wallet money budget finance pay bills"),
    AddShoppingCart(R.drawable.chore_icon_add_shopping_cart, "add shopping cart groceries buy store list"),
    Agriculture(R.drawable.chore_icon_agriculture, "agriculture garden farm tractor plant harvest field"),
    Air(R.drawable.chore_icon_air, "air wind fresh ventilate fan"),
    Alarm(R.drawable.chore_icon_alarm, "alarm clock wake up reminder time morning"),
    Assignment(R.drawable.chore_icon_assignment, "assignment task homework paperwork form document"),
    BabyChangingStation(R.drawable.chore_icon_baby_changing_station, "baby changing station diaper nappy change kids"),
    Backpack(R.drawable.chore_icon_backpack, "backpack school bag pack kids"),
    BakeryDining(R.drawable.chore_icon_bakery_dining, "bakery dining bake bread croissant breakfast food"),
    Bathroom(R.drawable.chore_icon_bathroom, "bathroom toilet clean bath shower"),
    Bathtub(R.drawable.chore_icon_bathtub, "bathtub bath tub clean bathroom wash"),
    BeachAccess(R.drawable.chore_icon_beach_access, "beach access umbrella holiday vacation summer"),
    Bed(R.drawable.chore_icon_bed, "bed make sheets bedroom sleep"),
    BedroomBaby(R.drawable.chore_icon_bedroom_baby, "bedroom baby nursery crib kids"),
    BedroomChild(R.drawable.chore_icon_bedroom_child, "bedroom child kids room tidy"),
    BedroomParent(R.drawable.chore_icon_bedroom_parent, "bedroom parent bed sheets sleep"),
    Bedtime(R.drawable.chore_icon_bedtime, "bedtime sleep night moon"),
    Blender(R.drawable.chore_icon_blender, "blender smoothie kitchen cook"),
    Blinds(R.drawable.chore_icon_blinds, "blinds window shades"),
    BlindsClosed(R.drawable.chore_icon_blinds_closed, "blinds closed window shades close"),
    Bolt(R.drawable.chore_icon_bolt, "bolt electricity power energy"),
    BreakfastDining(R.drawable.chore_icon_breakfast_dining, "breakfast dining toast food morning"),
    BrunchDining(R.drawable.chore_icon_brunch_dining, "brunch dining food drink meal"),
    Brush(R.drawable.chore_icon_brush, "brush paint decorate art"),
    BugReport(R.drawable.chore_icon_bug_report, "bug report insect pest"),
    Build(R.drawable.chore_icon_build, "build repair fix tools wrench diy maintenance"),
    Cake(R.drawable.chore_icon_cake, "cake bake birthday dessert party"),
    CalendarToday(R.drawable.chore_icon_calendar_today, "calendar today date plan schedule"),
    CarRepair(R.drawable.chore_icon_car_repair, "car repair mechanic service garage"),
    CardGiftcard(R.drawable.chore_icon_card_giftcard, "card giftcard gift present birthday wrap"),
    Carpenter(R.drawable.chore_icon_carpenter, "carpenter saw wood diy repair build"),
    Celebration(R.drawable.chore_icon_celebration, "celebration party celebrate event decorate"),
    Chair(R.drawable.chore_icon_chair, "chair furniture seat"),
    ChairAlt(R.drawable.chore_icon_chair_alt, "chair furniture seat dining"),
    Checklist(R.drawable.chore_icon_checklist, "checklist list todo tasks plan"),
    Checkroom(R.drawable.chore_icon_checkroom, "checkroom clothes hanger wardrobe closet coat"),
    ChildCare(R.drawable.chore_icon_child_care, "child care kids baby childcare babysit"),
    ChildFriendly(R.drawable.chore_icon_child_friendly, "child friendly stroller baby walk kids"),
    CleanHands(R.drawable.chore_icon_clean_hands, "clean hands wash hygiene"),
    CleaningServices(R.drawable.chore_icon_cleaning_services, "cleaning services clean spray wipe housework tidy dust chores mop floor sweep broom vacuum"),
    Coffee(R.drawable.chore_icon_coffee, "coffee tea drink cup"),
    CoffeeMaker(R.drawable.chore_icon_coffee_maker, "coffee maker machine descale kitchen"),
    Compost(R.drawable.chore_icon_compost, "compost garden waste organic"),
    Computer(R.drawable.chore_icon_computer, "computer pc work desk email"),
    Construction(R.drawable.chore_icon_construction, "construction build repair renovate diy"),
    ContentCut(R.drawable.chore_icon_content_cut, "content cut scissors haircut trim"),
    Cookie(R.drawable.chore_icon_cookie, "cookie bake snack"),
    Cottage(R.drawable.chore_icon_cottage, "cottage house home holiday"),
    Countertops(R.drawable.chore_icon_countertops, "countertops kitchen counter clean wipe"),
    Curtains(R.drawable.chore_icon_curtains, "curtains window wash"),
    CurtainsClosed(R.drawable.chore_icon_curtains_closed, "curtains closed window close"),
    Delete(R.drawable.chore_icon_delete, "delete trash bin rubbish garbage waste take out"),
    DeleteForever(R.drawable.chore_icon_delete_forever, "delete forever trash bin rubbish garbage throw away"),
    DeleteSweep(R.drawable.chore_icon_delete_sweep, "delete sweep trash clean declutter"),
    Desk(R.drawable.chore_icon_desk, "desk office work study tidy"),
    Dining(R.drawable.chore_icon_dining, "dining table eat meal"),
    DinnerDining(R.drawable.chore_icon_dinner_dining, "dinner dining cook meal pasta"),
    DirectionsCar(R.drawable.chore_icon_directions_car, "directions car drive errand trip"),
    DirectionsRun(R.drawable.chore_icon_directions_run, "directions run jog exercise fitness"),
    DirectionsWalk(R.drawable.chore_icon_directions_walk, "directions walk dog stroll exercise"),
    DoorBack(R.drawable.chore_icon_door_back, "door back exit"),
    DoorFront(R.drawable.chore_icon_door_front, "door front entrance"),
    DoorSliding(R.drawable.chore_icon_door_sliding, "door sliding patio"),
    Doorbell(R.drawable.chore_icon_doorbell, "doorbell door bell visitor"),
    Dry(R.drawable.chore_icon_dry, "dry hair towel"),
    DryCleaning(R.drawable.chore_icon_dry_cleaning, "dry cleaning clothes suit iron"),
    Eco(R.drawable.chore_icon_eco, "eco green leaf environment recycle"),
    EditNote(R.drawable.chore_icon_edit_note, "edit note write list plan"),
    Egg(R.drawable.chore_icon_egg, "egg cook breakfast food"),
    EggAlt(R.drawable.chore_icon_egg_alt, "egg fry cook breakfast"),
    Elderly(R.drawable.chore_icon_elderly, "elderly grandparent care visit"),
    EnergySavingsLeaf(R.drawable.chore_icon_energy_savings_leaf, "energy savings leaf save electricity green"),
    Event(R.drawable.chore_icon_event, "event calendar appointment plan"),
    Face(R.drawable.chore_icon_face, "face skincare self care person"),
    FamilyRestroom(R.drawable.chore_icon_family_restroom, "family restroom kids parents"),
    Fireplace(R.drawable.chore_icon_fireplace, "fireplace fire wood heating chimney"),
    FitnessCenter(R.drawable.chore_icon_fitness_center, "fitness center gym workout exercise weights"),
    Flatware(R.drawable.chore_icon_flatware, "flatware cutlery dishes kitchen table set"),
    FoodBank(R.drawable.chore_icon_food_bank, "food bank donate charity"),
    Forest(R.drawable.chore_icon_forest, "forest trees nature hike"),
    FormatPaint(R.drawable.chore_icon_format_paint, "format paint roller decorate walls"),
    FreeBreakfast(R.drawable.chore_icon_free_breakfast, "free breakfast coffee tea cup"),
    Garage(R.drawable.chore_icon_garage, "garage car tidy storage"),
    Grass(R.drawable.chore_icon_grass, "grass lawn mow garden"),
    Handshake(R.drawable.chore_icon_handshake, "handshake meeting help agreement"),
    Handyman(R.drawable.chore_icon_handyman, "handyman repair fix tools maintenance diy"),
    Hardware(R.drawable.chore_icon_hardware, "hardware hammer tools repair diy"),
    Healing(R.drawable.chore_icon_healing, "healing health first aid bandage"),
    HeatPump(R.drawable.chore_icon_heat_pump, "heat pump heating boiler service"),
    Hiking(R.drawable.chore_icon_hiking, "hiking hike walk outdoors"),
    Home(R.drawable.chore_icon_home, "home house"),
    HotTub(R.drawable.chore_icon_hot_tub, "hot tub jacuzzi spa clean"),
    HourglassBottom(R.drawable.chore_icon_hourglass_bottom, "hourglass bottom time wait timer"),
    House(R.drawable.chore_icon_house, "house home clean"),
    HouseSiding(R.drawable.chore_icon_house_siding, "house siding exterior wall paint"),
    Icecream(R.drawable.chore_icon_icecream, "icecream ice cream dessert treat"),
    ImagesearchRoller(R.drawable.chore_icon_imagesearch_roller, "imagesearch roller paint decorate"),
    Inventory(R.drawable.chore_icon_inventory, "inventory stock storage supplies"),
    Inventory2(R.drawable.chore_icon_inventory_2, "inventory2 boxes storage moving declutter"),
    Iron(R.drawable.chore_icon_iron, "iron ironing clothes laundry"),
    Kayaking(R.drawable.chore_icon_kayaking, "kayaking kayak boat water sport"),
    KebabDining(R.drawable.chore_icon_kebab_dining, "kebab dining food grill"),
    Key(R.drawable.chore_icon_key, "key lock keys"),
    KingBed(R.drawable.chore_icon_king_bed, "king bed sheets bedroom"),
    Kitchen(R.drawable.chore_icon_kitchen, "kitchen fridge refrigerator clean cook food groceries defrost"),
    Light(R.drawable.chore_icon_light, "light lamp bulb"),
    Lightbulb(R.drawable.chore_icon_lightbulb, "lightbulb light bulb lamp replace idea"),
    Liquor(R.drawable.chore_icon_liquor, "liquor drinks bottles alcohol"),
    LocalCarWash(R.drawable.chore_icon_local_car_wash, "car wash clean"),
    LocalDining(R.drawable.chore_icon_local_dining, "dining cook dinner meal"),
    LocalFlorist(R.drawable.chore_icon_local_florist, "florist flowers garden water plants"),
    LocalGasStation(R.drawable.chore_icon_local_gas_station, "gas station fuel petrol car refuel"),
    LocalGroceryStore(R.drawable.chore_icon_local_grocery_store, "grocery store groceries shopping food"),
    LocalLaundryService(R.drawable.chore_icon_local_laundry_service, "laundry service washing machine clothes wash"),
    LocalMall(R.drawable.chore_icon_local_mall, "mall shopping store"),
    LocalPizza(R.drawable.chore_icon_local_pizza, "pizza food dinner"),
    LocalPostOffice(R.drawable.chore_icon_local_post_office, "post office mail parcel letter"),
    LocalShipping(R.drawable.chore_icon_local_shipping, "shipping delivery truck parcel moving"),
    Lock(R.drawable.chore_icon_lock, "lock security door"),
    Luggage(R.drawable.chore_icon_luggage, "luggage suitcase pack travel trip"),
    LunchDining(R.drawable.chore_icon_lunch_dining, "lunch dining burger food meal"),
    Mail(R.drawable.chore_icon_mail, "mail post letter email"),
    MedicalServices(R.drawable.chore_icon_medical_services, "medical services doctor first aid health"),
    Medication(R.drawable.chore_icon_medication, "medication medicine pills health"),
    MedicationLiquid(R.drawable.chore_icon_medication_liquid, "medication liquid medicine syrup health"),
    MenuBook(R.drawable.chore_icon_menu_book, "menu book recipe cook read"),
    Microwave(R.drawable.chore_icon_microwave, "microwave kitchen clean heat"),
    MonitorWeight(R.drawable.chore_icon_monitor_weight, "monitor weight scale health"),
    MoveToInbox(R.drawable.chore_icon_move_to_inbox, "move to inbox sort mail"),
    MusicNote(R.drawable.chore_icon_music_note, "music note practice instrument"),
    Nature(R.drawable.chore_icon_nature, "nature tree garden"),
    NaturePeople(R.drawable.chore_icon_nature_people, "nature people outdoors park walk"),
    Nightlight(R.drawable.chore_icon_nightlight, "nightlight night moon sleep"),
    OutdoorGrill(R.drawable.chore_icon_outdoor_grill, "outdoor grill bbq barbecue cook outdoors"),
    Palette(R.drawable.chore_icon_palette, "palette art paint craft"),
    Park(R.drawable.chore_icon_park, "park walk tree outdoors"),
    Payments(R.drawable.chore_icon_payments, "payments pay bills money"),
    PedalBike(R.drawable.chore_icon_pedal_bike, "pedal bike bicycle cycle ride"),
    PestControl(R.drawable.chore_icon_pest_control, "pest control insects bug spray"),
    PestControlRodent(R.drawable.chore_icon_pest_control_rodent, "pest control rodent mouse rat trap"),
    Pets(R.drawable.chore_icon_pets, "pets dog cat feed walk vet"),
    Phone(R.drawable.chore_icon_phone, "phone call ring"),
    Piano(R.drawable.chore_icon_piano, "piano practice music lesson"),
    Plumbing(R.drawable.chore_icon_plumbing, "plumbing pipe leak repair wrench"),
    Pool(R.drawable.chore_icon_pool, "pool swim clean"),
    Print(R.drawable.chore_icon_print, "print printer paper"),
    Psychology(R.drawable.chore_icon_psychology, "psychology mind mental health think"),
    RamenDining(R.drawable.chore_icon_ramen_dining, "ramen dining noodles soup food"),
    Receipt(R.drawable.chore_icon_receipt, "receipt bill expenses"),
    ReceiptLong(R.drawable.chore_icon_receipt_long, "receipt long bills expenses budget"),
    Recycling(R.drawable.chore_icon_recycling, "recycling bin plastic paper glass"),
    Restaurant(R.drawable.chore_icon_restaurant, "restaurant eat cutlery"),
    RestaurantMenu(R.drawable.chore_icon_restaurant_menu, "restaurant menu meal plan cook"),
    RiceBowl(R.drawable.chore_icon_rice_bowl, "rice bowl food meal"),
    RollerShades(R.drawable.chore_icon_roller_shades, "roller shades blinds window"),
    Roofing(R.drawable.chore_icon_roofing, "roofing roof gutters repair house"),
    Sanitizer(R.drawable.chore_icon_sanitizer, "sanitizer disinfect hygiene clean"),
    Savings(R.drawable.chore_icon_savings, "savings piggy bank money budget"),
    Scale(R.drawable.chore_icon_scale, "scale weight kitchen"),
    School(R.drawable.chore_icon_school, "school homework study learn"),
    SelfImprovement(R.drawable.chore_icon_self_improvement, "self improvement meditation yoga relax"),
    SetMeal(R.drawable.chore_icon_set_meal, "set meal fish food cook"),
    Shelves(R.drawable.chore_icon_shelves, "shelves bookcase dust organize"),
    ShoppingBag(R.drawable.chore_icon_shopping_bag, "shopping bag buy"),
    ShoppingBasket(R.drawable.chore_icon_shopping_basket, "shopping basket groceries"),
    ShoppingCart(R.drawable.chore_icon_shopping_cart, "shopping cart groceries supermarket"),
    Shower(R.drawable.chore_icon_shower, "shower bathroom clean"),
    SingleBed(R.drawable.chore_icon_single_bed, "single bed sheets bedroom"),
    Snowing(R.drawable.chore_icon_snowing, "snowing snow shovel winter"),
    Soap(R.drawable.chore_icon_soap, "soap wash hands clean"),
    SoupKitchen(R.drawable.chore_icon_soup_kitchen, "soup kitchen cook pot"),
    Spa(R.drawable.chore_icon_spa, "spa relax self care rest breathe break tired sigh"),
    Sports(R.drawable.chore_icon_sports, "sports sport exercise game"),
    SportsBar(R.drawable.chore_icon_sports_bar, "sports bar drinks beer"),
    SportsBasketball(R.drawable.chore_icon_sports_basketball, "sports basketball sport exercise"),
    SportsSoccer(R.drawable.chore_icon_sports_soccer, "sports soccer football sport"),
    SportsTennis(R.drawable.chore_icon_sports_tennis, "sports tennis sport"),
    Storage(R.drawable.chore_icon_storage, "storage organize boxes"),
    Straighten(R.drawable.chore_icon_straighten, "straighten measure ruler diy"),
    Stroller(R.drawable.chore_icon_stroller, "stroller baby walk"),
    TableBar(R.drawable.chore_icon_table_bar, "table bar furniture"),
    TableRestaurant(R.drawable.chore_icon_table_restaurant, "table restaurant dining set wipe"),
    TakeoutDining(R.drawable.chore_icon_takeout_dining, "takeout dining takeaway leftovers food"),
    TaskAlt(R.drawable.chore_icon_task_alt, "task done complete check"),
    Thermostat(R.drawable.chore_icon_thermostat, "thermostat temperature heating"),
    Timer(R.drawable.chore_icon_timer, "timer stopwatch time"),
    TireRepair(R.drawable.chore_icon_tire_repair, "tire repair tyre car bike pump"),
    Today(R.drawable.chore_icon_today, "today date calendar"),
    Toys(R.drawable.chore_icon_toys, "toys kids tidy play"),
    Umbrella(R.drawable.chore_icon_umbrella, "umbrella rain"),
    Vaccines(R.drawable.chore_icon_vaccines, "vaccines vaccine doctor health"),
    VolunteerActivism(R.drawable.chore_icon_volunteer_activism, "volunteer activism charity help care"),
    Wallet(R.drawable.chore_icon_wallet, "wallet money"),
    Wash(R.drawable.chore_icon_wash, "wash dishes hands sink"),
    Water(R.drawable.chore_icon_water, "water drink"),
    WaterDrop(R.drawable.chore_icon_water_drop, "water drop plants leak"),
    WbSunny(R.drawable.chore_icon_wb_sunny, "sunny sun weather outdoors"),
    Wc(R.drawable.chore_icon_wc, "wc toilet bathroom clean"),
    Weekend(R.drawable.chore_icon_weekend, "weekend sofa couch living room"),
    Window(R.drawable.chore_icon_window, "window clean glass"),
    Yard(R.drawable.chore_icon_yard, "yard garden lawn plants weed");

    private final int DrawableId;
    public int GetDrawableId() { return DrawableId; }
    // Lowercase keywords: the words of the icon's name plus common chore words.
    private final String[] Tags;
    public String[] GetTags() { return Tags; }

    // Icons removed from the app, and the icon that replaces each of them in saved chores.
    private static final String[][] RemovedIcons = {
            { "Cleaning", "CleaningServices" },
            { "Sigh", "Spa" },
            { "FloorMop", "CleaningServices" },
            { "Fridge", "Kitchen" },
    };

    // The icon for a name read from saved data; also accepts the names of removed icons.
    public static ChoreIconEnum FromSavedName(String name)
    {
        for (String[] removed : RemovedIcons)
        {
            if (removed[0].equals(name))
                return valueOf(removed[1]);
        }
        return valueOf(name);
    }

    ChoreIconEnum(int drawableId, String tags)
    {
        DrawableId = drawableId;
        Tags = tags.split(" ");
    }

    // True when every word of the search text starts one of this icon's tags (case-insensitive),
    // e.g. "dish" or "wash dish" matches Wash. Empty search text matches every icon.
    public boolean MatchesSearch(String searchText)
    {
        for (String word : searchText.toLowerCase(Locale.ROOT).trim().split("\\s+"))
        {
            if (word.isEmpty())
                continue;
            boolean found = false;
            for (String tag : Tags)
            {
                if (tag.startsWith(word))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }
}
