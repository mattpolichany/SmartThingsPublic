/**
 *  Smart Screens
 *
 *  Copyright 2015 Martin Verbeek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *	App is using forecast.io to get conditions for your location, you need to get an APIKEY
 * 	from developer.forecast.io and your longitude and latitude is being pulled from the location object
 *  position of the sun is calculated in this app, thanks to the formaula´s of Mourner at suncalc github
 *
 *	Select the blinds you want to configure (it will use commands Open/Stop/Close) so they need to be on the device.
 *  Select the position they are facing (North-East-South-West) or multiple...these are the positions that they will need protection from wind or sun
 *  WindForce protection Select condition to Close or to Open (Shutters you may want to close when lots of wind, sunscreens you may want to Open
 *  cloudCover percentage is the condition to Close (Sun is shining into your room)
 *  Select interval to check conditions
 * 
 *	V2	Add subscription to device refresh, this will execute the latest info back to the devices
 	V3.00 move to windowShade capability, and issue createchild command to create related component device for smart screens
 
 */


import groovy.json.*
import java.Math.*
private def runningVersion() 	{"3.00"}

definition(
    name: "Smart Screens",
    namespace: "verbem",
    author: "Martin Verbeek",
    description: "Automate Up and Down of Sun Screens, Blinds and Shutters based on Weather Conditions",
    category: "Convenience",
    oauth: true,
    iconUrl: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/smart-screens.src/Smart%20Screens.jpg",
    iconX2Url: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/smart-screens.src/Smart%20Screens.jpg",
    iconX3Url: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/smart-screens.src/Smart%20Screens.jpg")

{
	appSetting "clientId"
	appSetting "clientSecret"
    appSetting "serverUrl"
}


mappings {
    path("/oauth/initialize") {action: [GET: "getNetatmoAuth"]}
    path("/oauth/callback") {action: [GET: "getNetatmoCallback"]}
}

preferences {
    page name:"pageSetupForecastIO"
    page name:"pageConfigureBlinds"
    page name:"pageForecastIO"
}

def pageSetupForecastIO() {
    TRACE("pageSetupForecastIO()")

    def pageSetupLatitude = location.latitude.toString()
    def pageSetupLongitude = location.longitude.toString()
	
    def pageSetupAPI = [
        name:       "pageSetupAPI",
        type:       "string",
        title:      "API key(darksky), key(WU) or APPID(OWM)",
        multiple:   false,
        required:   true
    	]

	def inputWeather = [
        name:       "z_weatherAPI",
        type:       "enum",
        options:	["Darksky", "OpenWeatherMap", "WeatherUnderground", "WeatherUnderground-NoPWS"],
        title:      "Select Weather API",
        multiple:   false,
        submitOnChange: true,
        required:   true
        ]
              
    def inputBlinds = [
        name:       "z_blinds",
        type:       "capability.windowShade",
        title:      "Which blinds/screens/shutters?",
        multiple:   true,
        required:   false
    ] 
    
    def pageProperties = [
        name:       "pageSetupForecastIO",
        //title:      "Status",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    def inputWindForceMetric = [
        name:       "z_windForceMetric",
        type:       "enum",
        options:	["mph", "km/h"],
        title:      "Select wind metric system",
        multiple:   false,
        required:   true
    ]
    
        def inputTRACE = [
        name:       "z_TRACE",
        type:       "bool",
        default:	false,
        title:      "Put out trace log",
        multiple:   false,
        required:   true
    ]

        def inputPause = [
        name:       "z_Pause",
        type:       "bool",
        default:	false,
        title:      "Pause all scheduling",
        multiple:   false,
        required:   true
    ]
    
    def inputSensors = [
        name:       "z_sensors",
        type:       "capability.sensor",
        title:      "Which NETATMO wind devices?",
        multiple:   true,
        required:   false
    ] 
    
    return dynamicPage(pageProperties) {
		
        section("Darksky.net, WeatherUndergound or OpenWeatherMap API Key and Website") {
        	input inputWeather
        	
            if (z_weatherAPI) {
                input pageSetupAPI

				if (z_weatherAPI == "Darksky") {
                    href(name: "hrefNotRequired",
                     title: "Darksky.net page",
                     required: false,
                     style: "external",
                     url: "https://darksky.net/dev/",
                     description: "tap to view Darksky website in mobile browser")
                    }

				if (z_weatherAPI == "WeatherUnderground" || z_weatherAPI == "WeatherUnderground-NoPWS") {
                    href(name: "hrefNotRequired",
                     title: "WeatherUnderground page",
                     required: false,
                     style: "external",
                     url: "https://www.wunderground.com/weather/api/d/pricing.html",
                     description: "tap to view WU website in mobile browser")
                    }

				if (z_weatherAPI == "OpenWeatherMap") {
                    href(name: "hrefNotRequired",
                     title: "OpenWeatherMap page",
                     required: false,
                     style: "external",
                     url: "https://home.openweathermap.org/users/sign_in",
                     description: "tap to view OWM website in mobile browser")
                     }
			}
            
        }



        section("Netatmo Interface") {  
        	input inputSensors
		}
        
        section("Setup Menu") {
            input inputBlinds

            if(inputBlinds) {
                    href "pageConfigureBlinds", title:"Configure Blinds", description:"Tap to open"
                    input inputWindForceMetric
                }
        }
        
        section("Info Page") {

              href "pageForecastIO", title:"Environment Info", description:"Tap to open"
              
        }

        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
            input inputTRACE
            input inputPause
        }
    }
}

/*-----------------------------------------------------------------------*/
// Show Configure Blinds Page
/*-----------------------------------------------------------------------*/

def pageConfigureBlinds() {
    TRACE("pageConfigureBlinds()")

def pageProperties = [
        name:       "pageConfigureBlinds",
        title:      "Configure Blinds",
        nextPage:   "pageSetupForecastIO",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        z_blinds.each {
        	def devId = it.id
            def blind = it.currentValue("somfySupported")
            if (blind == 'true') {blind = true}
            	else {blind = false}
                
            section(it.name) 
            	{           	
                input 	"z_blindsOrientation_${devId}", "enum", options:["N", "NW", "W", "SW", "S", "SE", "E", "NE"],title:"Select Relevent Orientation",multiple:true,required:true
                input	"z_blindType_${devId}", "enum", options:["Screen","Shutter"], title:"(sun)Screen or (roller)Shutter", required:true, multiple:false, submitOnChange:true
                
                if (settings."z_blindType_${devId}" == "Screen") {
                    input	"z_windForceCloseMax_${devId}","number",title:"Allow Operation below under which windforce ${z_windForceMetric}",multiple:false,required:false,default:0                 

                    if (blind) 	{input	"z_closeMaxAction_${devId}","enum",title:"", options: ["Down","Up","Preset","Dynamic"], default:"Preset" }
                    else 		{input	"z_closeMaxAction_${devId}","enum",title:"", options: ["Down","Up","Dynamic"], default:"Up" }
					}

					if (settings."z_blindType_${devId}" == "Shutter") {

                    if (blind) 	{input	"z_closeMaxAction_${devId}","enum",title:"Action for Sun protection", options: ["Down","Up","Preset","Dynamic"], default:"Preset" }
                    else 		{input	"z_closeMaxAction_${devId}","enum",title:"Action for Sun protection", options: ["Down","Up","Dynamic"], default:"Up" }
					}

                input 	"z_cloudCover_${devId}","enum",title:"Protect until what cloudcover% (0=clear sky)", options:	["10","20","30","40","50","60","70","80","90","100"],multiple:false,required:false,default:30                

				if (settings."z_blindType_${devId}" == "Shutter") {
                    input 	"z_windForceCloseMin_${devId}","number",title:"Force Operation below above which windforce ${z_windForceMetric}",multiple:false,required:false,default:999                     

                    if (blind) 	{input	"z_closeMinAction_${devId}","enum",title:"", options: ["Down","Up","Preset"], default:"Preset" }
                    else 		{input	"z_closeMinAction_${devId}","enum",title:"", options: ["Down","Up"], default:"Up" }
                	}
                
                input	"z_sunsetOffset_${devId}","number",title:"Operation below at Sunset +/- offset",multiple:false,required:false,default:0                 
                
                if (blind) 	{input	"z_eodAction_${devId}","enum",title:"", options: ["Down","Up","Preset"], default:"Preset" }
                else if (settings."z_blindType_${devId}" == "Shutter")	{input	"z_eodAction_${devId}","enum",title:"", options: ["Down","Up"], default:"Down" }
                		else {input	"z_eodAction_${devId}","enum",title:"", options: ["Down","Up"], default:"Up" }
                
                    
               	input "z_blindsOpenSensor_${devId}", "capability.contactSensor", required:false, multiple:false, title:"No operation when open"

        	}
        }
    }
}

/*-----------------------------------------------------------------------*/
// Show Sun/Wind ForecastIO API last output Page
/*-----------------------------------------------------------------------*/

def pageForecastIO() {
    TRACE("pageForecastIO()")
    state.sunBearing = getSunpath()
    def sc = sunCalc()

    def pageProperties = [
            name:       "pageForecastIO",
            title:      "Current Sun and Wind Info",
            nextPage:   "pageSetupForecastIO",
            refreshInterval: 10,
            uninstall:  false
        ]    
   
    return dynamicPage(pageProperties) {

        section("Wind") {
        	paragraph "Speed ${state.windSpeed} from direction ${state.windBearing}" 
            z_sensors.each {
            	paragraph "${it.displayName} speed ${it.currentValue("WindStrength")} from direction ${calcBearing(it.currentValue("WindAngle"))}"
            }
		}
        
        section("Sun") {
            paragraph "cloud Cover ${state.cloudCover} Sun in direction ${state.sunBearing}"
		}
        
        section("SunCalc") {
        	paragraph "Latitude ${state.lat}"
        	paragraph "Longitude ${state.lng}"
            paragraph "Suncoord ${state.c}"
            paragraph "Azimuth ${sc.azimuth}"
            paragraph "Altitude ${sc.altitude}"
		}
    }
}

def installed() {
	TRACE("Installed with settings: ${settings}")

	initialize()
}

def updated() {
	TRACE("Updated with settings: ${settings}")

	unsubscribe()
	initialize()
}

def initialize() {
	
    state.sunBearing = ""
    state.windBearing = ""
    state.night = false
    state.windSpeed = 0
    state.cloudCover = 100
        
	subscribe(location, "sunsetTime", sunsetTimeHandler)
	subscribe(location, "sunset", stopSunpath,  [filterEvents:true])
    subscribe(location, "sunrise", startSunpath,  [filterEvents:true])
    
    schedule("2015-01-09T12:00:00.000-0600", notifyNewVersion)

	def blindParams = [:]
    settings.z_blinds.each {
    	blindParams = fillBlindParams(it.id)
        if (blindParams.sunsetOffset == "") {settings."z_sunsetOffset_${it.id}" = 0} 
        scheduleTurnOn(location.currentValue("sunsetTime"), it, settings."z_sunsetOffset_${it.id}", blindParams.eodAction)
		}	
  
    subscribe(z_sensors, "WindStrength", eventNetatmo)
    
    settings.z_blinds.each {
    	subscribeToCommand(it, "refresh", eventRefresh)
        }
    
    settings.each { k, v ->
    	if (k.contains("z_blindsOpenSensor")) {
        	subscribe(v, "contact.Closed", eventDoorClosed) 
        }
    }
    
    checkForWind()
    checkForSun()
    checkForClouds()
    
    runEvery30Minutes(checkForSun)
    runEvery1Hour(checkForClouds)
    runEvery10Minutes(checkForWind)
}

def eventDoorClosed(evt) {
	TRACE("[eventDoorClosed] ${evt.device} has closed") 
 	
    if (state.night == false) checkForSun()

}
/*-----------------------------------------------------------------------------------------*/
/*	This routine will get information relating to the weather at location and current time
/*-----------------------------------------------------------------------------------------*/
def getForecast() {

	def windBearing
    def windSpeed
    def cloudCover
    def returnList = [:]
    
    TRACE("[getForecast] ${settings.z_weatherAPI} for Lon:${location.longitude} Lat:${location.latitude}")

	if (settings.z_weatherAPI == "Darksky") {
    	def units = "si"
	    if (settings.z_windForceMetric == "mph") {units = "us"}
		def httpGetParams = [
            uri: "https://api.darksky.net",
            path: "/forecast/${settings.pageSetupAPI}/${location.latitude},${location.longitude}",
            contentType: "application/json", 
            query: ["units" : units, "exclude" : "minutely,hourly,daily,flags"]
        ]
        try {
            httpGet(httpGetParams) { response ->
                returnList.put('windBearing' ,calcBearing(response.data.currently.windBearing))
                returnList.put('windSpeed', Math.round(response.data.currently.windSpeed.toDouble()))
                returnList.put('cloudCover', response.data.currently.cloudCover.toDouble() * 100)
                }
            } 
            catch (e) {
                log.error "DARKSKY something went wrong: $e"
				returnList = [:]
            }
	}
    
	if (settings.z_weatherAPI == "OpenWeatherMap") {
    	def units = "metric"
	    if (settings.z_windForceMetric == "mph") {units = "imperial"}
        def httpGetParams = "http://api.openweathermap.org/data/2.5/weather?lat=${location.latitude}&lon=${location.longitude}&APPID=${settings.pageSetupAPI}&units=${units}"
        try {
            httpGet(httpGetParams) { resp ->
                returnList.put('windBearing',calcBearing(resp.data.wind.deg))
                returnList.put('windSpeed', Math.round(resp.data.wind.speed.toDouble()))
                returnList.put('cloudCover', resp.data.clouds.all.toDouble())
            	}
            } 
            catch (e) {
                log.error "OWM something went wrong: $e"
				returnList = [:]
            }
		}

	if (settings.z_weatherAPI.contains("WeatherUnderground")) {
		def httpGetParams = "http://api.wunderground.com/api/${settings.pageSetupAPI}/conditions/pws:0/q/${location.latitude},${location.longitude}.json"

		if (settings.z_weatherAPI.contains("NoPWS") == false) {
            httpGetParams = "http://api.wunderground.com/api/${settings.pageSetupAPI}/conditions/pws:1/q/${location.latitude},${location.longitude}.json"
        	TRACE("[getForecast] Use PWS is true ${httpGetParams}")
            }
        else {
        	TRACE("[getForecast] Use PWS is false ${httpGetParams}")
            }
            
		try {
            httpGet(httpGetParams) { resp ->
                returnList.put('windBearing',calcBearing(resp.data.current_observation.wind_degrees))
                returnList.put('windSpeed', resp.data.current_observation.wind_kph.toDouble())  //all others do m/s if metric, account for this.
                def CC = 100
                switch (resp.data.current_observation.weather) {
                case ["Clear"]:
                    CC = 0
                    break;
                case "Scattered Clouds":
                    CC = 30
                    break;
                case "Partly Cloudy":
                    CC = 50
                    break;
                case ["Mostly Cloudy", "Overcast"]:
                    CC = 80
                    break;
                default:
                    CC = 100
                    break
                	}
                returnList.put('cloudCover', CC.toDouble())
            	}
          	} 
            catch (e) {
                log.error "WU something went wrong: $e"
                log.error "WU ${httpGetParams}"
				returnList = [:]
            } 
		}
TRACE("[getForecast] ${settings.z_weatherAPI} ${returnList}")
return returnList
}

/*-----------------------------------------------------------------------------------------*/
/*	This routine will get information relating to the SUN´s position
/*-----------------------------------------------------------------------------------------*/
def getSunpath() {
    TRACE("[getSunpath]")
    def sp = sunCalc()
    return calcBearing(sp.azimuth)  
}

/*-----------------------------------------------------------------------------------------*/
/*	This is a scheduled event that will get latest SUN related info on position
/*	and will check the blinds that provide sun protection if they need to be closed or opened
/*-----------------------------------------------------------------------------------------*/
def checkForSun(evt) {

if (settings.z_Pause) return

TRACE("[checkForSun]")
state.sunBearing = getSunpath()

settings.z_blinds.each {

    def blindParams = fillBlindParams(it.id)
	def dev = blindParams.blindsOpenSensor
    def devStatus = "Closed"
    if (dev) {
    	devStatus = dev.currentValue("contact")
    	}
    def eodDone = it.currentValue("eodDone")

	/*-----------------------------------------------------------------------------------------*/
    /*	SUN determine if we need to close or open again if cloudcover above defined
    /*		Only if SUN is in position to shine onto the blinds
    /*		Only if windspeed is below defined point (above it may damage sun screens)
    /*			this is only needed if direction of wind is on the screens
    /*	 
    /*-----------------------------------------------------------------------------------------*/                 
    TRACE("[checkForSun] ${it} has ${blindParams.blindsOrientation.contains(state.sunBearing)} sun orientation, DOOR ${dev} is ${devStatus}, EOD ${eodDone}, ACTION to take ${blindParams.closeMaxAction}")
    
    if(blindParams.blindsOrientation.contains(state.sunBearing) && devStatus == "Closed" && eodDone == 'false' ) 
    {
    	TRACE("[checkForSun] ${it} Forecast is ${state.cloudCover.toInteger()}% cloud, BLINDPARAMS is ${blindParams.cloudCover.toInteger()}%")
        
        if(state.cloudCover.toInteger() <= blindParams.cloudCover.toInteger()) 
        {           
            TRACE("[checkForSun] ${it} Forecasted ${state.windSpeed.toInteger()} < ${blindParams.windForceCloseMax.toInteger()}, wind orientation ${blindParams.blindsOrientation.contains(state.windBearing)}, Type is ${blindParams.blindsType}")
            
                	if (blindParams.blindsType == "Screen")                     
                		{
                        if((state.windSpeed.toInteger() < blindParams.windForceCloseMax.toInteger() && blindParams.blindsOrientation.contains(state.windBearing)) || blindParams.blindsOrientation.contains(state.windBearing) == false )
                   	    	{
                            if (blindParams.closeMaxAction == "Down") 	{it.close()}
                    	    if (blindParams.closeMaxAction == "Up") 	{it.open()}
                        	if (blindParams.closeMaxAction == "Preset") {it.presetPosition()}
                           	}
                        }
                	else // shutter
                		{
                    	if (blindParams.closeMaxAction == "Down") 	{it.close()}
                    	if (blindParams.closeMaxAction == "Up") 	{it.open()}
                    	if (blindParams.closeMaxAction == "Preset") {it.presetPosition()}
                    	}
        }
    }
}

return null
}

/*-----------------------------------------------------------------------------------------*/
/*	This is a scheduled event that will get latest SUN related info on position
/*	and will check the blinds that provide sun protection if they need to be opened
/*-----------------------------------------------------------------------------------------*/
def checkForClouds() {

if (settings.z_Pause) return

TRACE("[checkForClouds] ${params}")
state.sunBearing = getSunpath()

settings.z_blinds.each {

    def blindParams = fillBlindParams(it.id)     
	/*-----------------------------------------------------------------------------------------*/
    /*	SUN determine if we need to close or open again if cloudcover above defined
    /*		Only if SUN is in position to shine onto the blinds
    /*		Only if windspeed is below defined point (above it may damage sun screens)
    /*			this is only needed if direction of wind is on the screens
    /*	 
    /*-----------------------------------------------------------------------------------------*/                 
    if(blindParams.blindsOrientation.contains(state.sunBearing)) 
    {
        if(state.cloudCover.toInteger() > blindParams.cloudCover.toInteger() && blindParams.blindsType == "Screen") 
        {
			if (blindParams.closeMaxAction == "Down") it.open()
            if (blindParams.closeMaxAction == "Up") it.close()
            if (blindParams.closeMaxAction == "Preset") it.open()
        }
        if(state.cloudCover.toInteger() > blindParams.cloudCover.toInteger() && blindParams.blindsType == "Shutter") 
        {
			if (blindParams.closeMaxAction == "Down") it.open()
            if (blindParams.closeMaxAction == "Up") it.close()
            if (blindParams.closeMaxAction == "Preset") it.presetPosition()
        }
    }
}

return null
}
/*-----------------------------------------------------------------------------------------*/
/*	This is an event handler that will be provided with wind events from NETATMO devices
/*-----------------------------------------------------------------------------------------*/
def eventNetatmo(evt) {
	TRACE("[eventNetatmo]")

    def dev = evt.getDevice()
    def windAngle 		= calcBearing(dev.latestValue("WindAngle"))
    def gustStrength 	= dev.latestValue("GustStrength")
    def gustAngle 		= calcBearing(dev.latestValue("GustAngle"))
    def windStrength 	= dev.latestValue("WindStrength")
    def units 			= dev.latestValue("units")

    if (evt.isStateChange()) {
        state.windBearing = windAngle
        state.windSpeed = windStrength
        checkForWind("NETATMO")
    }
}

/*-----------------------------------------------------------------------------------------*/
/*	This is a scheduled event that will get latest WIND related info on position
/*	and will check the blinds if they need to be closed or can be opened
/*-----------------------------------------------------------------------------------------*/
def checkForWind(evt) {

    state.sunBearing = getSunpath()

    def windParms = [:]

    if (evt == null) {
        evt = settings.z_weatherAPI
        windParms = getForecast()
        state.windBearing = windParms.windBearing
        state.windSpeed = windParms.windSpeed
        if (settings.z_windForceMetric == "km/h" && settings.z_weatherAPI.contains("WeatherUnderground") == false) {state.windSpeed = windParms.windSpeed * 3.6}
        state.cloudCover = windParms.cloudCover
    }

    settings.z_blinds.each {
        it.generateEvent(["windBearing": state.windBearing, "windSpeed": state.windSpeed, "cloudCover": state.cloudCover, "sunBearing": state.sunBearing])
    }

    if (settings.z_Pause) return

    TRACE("[checkForWind]")

    settings.z_blinds.each {

        def blindParams = fillBlindParams(it.id)
        def dev = blindParams.blindsOpenSensor
        def devStatus = "Closed"
        if (dev) {
            devStatus = dev.currentValue("contact")
        }

        /*-----------------------------------------------------------------------------------------*/
        /*	WIND determine if we need to close (or OPEN if wind speed is above allowed max for blind)
        /*-----------------------------------------------------------------------------------------*/      
        if(blindParams.blindsOrientation.contains(state.windBearing) && devStatus == "Closed") {
            if(state.windSpeed.toInteger() > blindParams.windForceCloseMin.toInteger() && blindParams.blindsType == "Shutter") {
                if (blindParams.closeMinAction == "Down") it.close()
                if (blindParams.closeMinAction == "Up") it.open()
                if (blindParams.closeMinAction == "Preset") it.presetPosition()
            }
            if(state.windSpeed.toInteger() > blindParams.windForceCloseMax.toInteger() && blindParams.blindsType == "Screen") {
                //reverse the defined MaxAction
                if (blindParams.closeMaxAction == "Down") it.open()
                if (blindParams.closeMaxAction == "Up") it.close()
                if (blindParams.closeMaxAction == "Preset") it.open()
            }
        }
    }

    return null
}

def eventRefresh(evt) {
	if (state.night == true) {
        checkForWind()
    	return
    }
    
    TRACE("[eventRefresh] ${evt.device} Source ${evt.source}")
    checkForClouds()
    checkForSun()
    checkForWind()

}

/*-----------------------------------------------------------------------------------------*/
/*	this will stop the scheduling of events called at SUNSET
/*-----------------------------------------------------------------------------------------*/
def stopSunpath(evt) {
	TRACE("[stopSunpath] Stop Scheduling")
    state.night = true
    pause 5
	unschedule(checkForSun)
    unschedule(checkForClouds)
    unschedule(checkForWind)
    unsubscribe(eventRefresh)
    
	return null
}

/*-----------------------------------------------------------------------------------------*/
/*	this will start the scheduling of events called at SUNRISE
/*-----------------------------------------------------------------------------------------*/
def startSunpath(evt) {
	TRACE("[startSunpath] Start Scheduling")
    state.night = false
    pause 5
	runEvery30Minutes(checkForSun)
    runEvery3Hours(checkForClouds)
    runEvery10Minutes(checkForWind)
    
    z_blinds.each {
    	subscribeToCommand(it, "refresh", eventRefresh)
        }
    
	return null
}

def sunsetTimeHandler(evt) {

    def blindParams = [:]
	settings.z_blinds.each {
        String findID = it.id
    	blindParams = fillBlindParams(findID)
        scheduleTurnOn(evt.value, it, blindParams.sunsetOffset, blindParams.eodAction)     
	}
}

private def fillBlindParams(findID) {

	def blindParams = [:]
    blindParams.blindsOrientation = settings?."z_blindsOrientation_${findID}"
    blindParams.windForceCloseMax = settings?."z_windForceCloseMax_${findID}"
    blindParams.windForceCloseMin = settings?."z_windForceCloseMin_${findID}"
    blindParams.cloudCover = settings?."z_cloudCover_${findID}"
    blindParams.closeMinAction = settings?."z_closeMinAction_${findID}"    
    blindParams.closeMaxAction = settings?."z_closeMaxAction_${findID}"    
    blindParams.blindsType = settings?."z_blindType_${findID}"        
	blindParams.blindsOpenSensor = settings?."z_blindsOpenSensor_${findID}"
	blindParams.sunsetOffset = settings?."z_sunsetOffset_${findID}"
    blindParams.eodAction = settings?."z_eodAction_${findID}"

	return blindParams
}

def scheduleTurnOn(sunsetString, eodDevice, offset, eodAction) {
    if (offset == "" || offset == null) {offset = 0}
    
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    log.trace "${sunsetTime} / ${eodDevice} / ${offset}"


    offset = offset * 60 * 1000
    def timeBeforeSunset = new Date(sunsetTime.time + offset)

	eodDevice.generateEvent(["eodAction" : eodAction, "eodTime" : new Date(sunsetTime.time + offset)]) 
    pause 5
    eodDevice.eodRunOnce(timeBeforeSunset)

}

/*-----------------------------------------------------------------------------------------*/
/*	this routine will return the wind or sun direction
/*-----------------------------------------------------------------------------------------*/
private def calcBearing(degree) {
		
        switch (degree.toInteger()) {
        case 0..23:
            return "N"
            break;
        case 23..68:
            return "NE"
            break;
        case 68..113:
            return "E"
            break;
        case 113..158:
            return "SE"
            break;
        case 158..203:
            return "S"
            break;
        case 203..248:
            return "SW"
            break;
        case 248..293:
            return "W"
            break;
        case 293..338:
            return "NW"
            break;
        case 338..360:
            return "N"
            break;
		default :
        	return "not found"
        	break;
        } 
 
}
private def TRACE(message) {

if(settings.z_TRACE) {log.trace message}

}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Azimuth and Alitude for current Time
/*-----------------------------------------------------------------------------------------*/
def sunCalc() {

	def lng = location.longitude
   	def lat = location.latitude

    state.lat = lat
    state.lng = lng
    state.julian = toJulian()
    
    def lw  = rad() * -lng
    state.lw = lw
    
    def phi = rad() * lat
    state.phi = phi
    
    def d   = toDays()
    state.d = d

    def c  = sunCoords(d)
    state.c = c
    
    def H  = siderealTime(d, lw) - c.ra
    state.H = H
     
    def az = azimuth(H, phi, c.dec)
    az = (az*180/Math.PI)+180
    def al = altitude(H, phi, c.dec)
    al = al*180/Math.PI
    
    return [
        azimuth: az,
        altitude: al
    ]
}
/*-----------------------------------------------------------------------------------------*/
/*	Return the Julian date 
/*-----------------------------------------------------------------------------------------*/
def toJulian() { 
    def date = new Date()
    date = date.getTime() / dayMs() - 0.5 + J1970() // ms time/ms in a day = days - 0.5 + number of days 1970.... 
    return date   
}
/*-----------------------------------------------------------------------------------------*/
/*	Return the number of days since J2000
/*-----------------------------------------------------------------------------------------*/
def toDays(){ return toJulian() - J2000()}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun RA
/*-----------------------------------------------------------------------------------------*/
def rightAscension(l, b) { 
	return Math.atan2(Math.sin(l) * Math.cos(e()) - Math.tan(b) * Math.sin(e()), Math.cos(l)) 
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Declination
/*-----------------------------------------------------------------------------------------*/
def declination(l, b)    { return Math.asin(Math.sin(b) * Math.cos(e()) + Math.cos(b) * Math.sin(e()) * Math.sin(l)) } 
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Azimuth
/*-----------------------------------------------------------------------------------------*/
def azimuth(H, phi, dec)  { return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi)) }
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Altitude
/*-----------------------------------------------------------------------------------------*/
def altitude(H, phi, dec) { return Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H)) }
/*-----------------------------------------------------------------------------------------*/
/*	compute sidereal time (One sidereal day corresponds to the time taken for the Earth to rotate once with respect to the stars and lasts approximately 23 h 56 min.
/*-----------------------------------------------------------------------------------------*/
def siderealTime(d, lw) { return rad() * (280.16 + 360.9856235 * d) - lw }
/*-----------------------------------------------------------------------------------------*/
/*	Compute Sun Mean Anomaly
/*-----------------------------------------------------------------------------------------*/
def solarMeanAnomaly(d) { return rad() * (357.5291 + 0.98560028 * d) }
/*-----------------------------------------------------------------------------------------*/
/*	Compute Sun Ecliptic Longitude
/*-----------------------------------------------------------------------------------------*/
def eclipticLongitude(M) {

	def C = rad() * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M))
	def P = rad() * 102.9372 // perihelion of the Earth

    return M + C + P + Math.PI 
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Coordinates
/*-----------------------------------------------------------------------------------------*/
def sunCoords(d) {

    def M = solarMeanAnomaly(d)
    def L = eclipticLongitude(M)

	return [dec: declination(L, 0), ra: rightAscension(L, 0)]
}
/*-----------------------------------------------------------------------------------------*/
/*	Some auxilliary routines for readabulity in the code
/*-----------------------------------------------------------------------------------------*/
def dayMs() { return 1000 * 60 * 60 * 24 }
def J1970() { return 2440588}
def J2000() { return 2451545}
def rad() { return  Math.PI / 180}
def e() { return  rad() * 23.4397}

/*-----------------------------------------------------------------------------------------*/
/*	Version Control
/*-----------------------------------------------------------------------------------------*/
def getWebData(params, desc, text=true) {
	try {
		httpGet(params) { resp ->
			if(resp.data) {
				if(text) { return resp?.data?.text.toString() } 
                else { return resp?.data }
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {log.error "${desc} file not found"} 
        else { log.error "[getWebData] (params: $params, desc: $desc, text: $text) Exception:", ex}
		
        return "[getWebData] ${label} info not found"
	}
}

def notifyNewVersion() {

	if (appVerInfo().split()[1] != runningVersion()) {
    	sendNotificationEvent("Hue Sensor App has a newer version, ${appVerInfo().split()[1]}, please visit IDE to update app/devices")
    }
}

private def appVerInfo()		{ return getWebData([uri: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/SmartScreensData", contentType: "text/plain; charset=UTF-8"], "changelog") }