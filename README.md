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

### Navigation  
- NavigationHostFragment (mobile) hosted by activity_main.xml (mobile)  
![Alt text](/image/FlowChartMobile.jpg  "Mobile flowchart")
  

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
- Add friends button
- My statistics button
- My friends button
- Lounge button
- Join a race button

### LoungeFragment
- Map with your location and the goal location
- Start race button
- Add friend to race button
- Change the location of the goal 


### ResultFragment
- Display winner name and elapse time


### StatFragment
- Display your previous races


<p align="center" width="100%">
    <img width="30%" src=/image/Login.png> 
    <img width="30%" src=/image/myspace.png> 
    <img width="30%" src=/image/Lounge.png> 
</p>

<p align="center" width="100%">
    <img width="30%" src=/image/Results.png> 
    <img width="30%" src=/image/MyStatistics.png> 
    <img width="30%" src=/image/MyFriends.png> 
</p>

## Wear

### WaitingFragment

### StartFragment

### RaceFragment

### EndFragment


