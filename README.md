# green-red-light-app-android
Android application for playing the 1-2-3 soleil game

# Structure
### Main activity : Mobile
- MainActivity
- FriendsFragment
- FriendsViewModel
- InProgressFragment
- ItemAdapterFragment
- ItemAdapterStat
- LoginFragment
- MySpaceFragment
- LoungeFragment
- ResultFragment
- MultPlayerFragment
- SharedViewModel
- StatFragment
- StatViewModel

### Main activity : Wear
- MainActivity

### navigation  
- NavigationHostFragment (mobile) hosted by activity_main.xml (mobile)  
![Alt text](/image/MobileNavigaton.drawio.png "Mobile")
  
- NavigationHostFragment (wear) hosted by activity_main.xml (wear)  
![Alt text](/image/WearNavigaton.drawio.png "Wear")  

### FireBase

### DataFlow
![Alt text](/image/dataFlowGreenRedLight.drawio.png "data Flow")


# Fragment

## Mobile

### LoginFragment
- Fetch profile
- Create profile
- Sign up button
- Sign in button

### MySpaceFragment
- Stat button
- Lounge button

### LoungeFragment

### ResultFragment

### StatFragment

## Wear

### WaitingFragment

### StartFragment

### RaceFragment

### EndFragment


