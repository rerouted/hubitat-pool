/**
 * Custom NodeJS Pool Controller Driver
 *
 *  Copyright 2019 Ryan Sullivan
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
 *  Last Update 07/09/2019
 *
 *
 *  V1.0.0 - First Version @rerouted
 *
 */

metadata {
    definition (name: "Custom Pool Driver", namespace: "Rerouted", author: "Ryan Sullivan", importUrl: "https://raw.githubusercontent.com/rerouted/Hubitat/master/Drivers/Pool/xxx.groovy") {
        capability "Actuator"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "ThermostatHeatingSetpoint"
        

        command "poll"
        command "ForcePoll"
 	    command "ResetPollCount"
        attribute "pollsSinceReset", "number"
        attribute "temperatureUnit", "string"
        attribute "poolTemp", "number"
        attribute "airTemp", "number"
 		attribute "DriverAuthor", "string"
        attribute "DriverVersion", "string"
        attribute "DriverStatus", "string"
		attribute "DriverUpdate", "string"     
        
    }
    preferences() {
        section("Query Inputs"){
            input "serverAddress", "text", required: true, title: "Pool Server IP/Hostname"
            input "serverPort", "number", required: true, title: "Pool Server Port", defaultValue: 3000
			input "unitFormat", "enum", required: true, title: "Unit Format",  options: ["Imperial", "Metric"]
	        input "pollIntervalLimit", "number", title: "Poll Interval Limit:", required: true, defaultValue: 1
            input "autoPoll", "bool", required: false, title: "Enable Auto Poll"
            input "pollInterval", "enum", title: "Auto Poll Interval:", required: false, defaultValue: "5 Minutes", options: ["5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "logSet", "bool", title: "Enable Logging", required: true, defaultValue: false
        }
    }
}

def updated() {
    log.debug "updated called"
   updateCheck()
    unschedule()
    version()
    state.NumOfPolls = 0
    ForcePoll()
    def pollIntervalCmd = (settings?.pollInterval ?: "5 Minutes").replace(" ", "")
    if(autoPoll)
        "runEvery${pollIntervalCmd}"(pollSchedule)
    
     def changeOver = cutOff
    schedule(changeOver, ResetPollCount)
    if(logSet){runIn(1800, logsOff)}
}

def ResetPollCount(){
state.NumOfPolls = -1
    log.info "Poll counter reset.."
ForcePoll()
}

def pollSchedule()
{
    ForcePoll()
}
              
def parse(String description) {
}

def poll()
{
    if(now() - state.lastPoll > (pollIntervalLimit * 60000))
        ForcePoll()
    else
        log.debug "Poll called before interval threshold was reached"
}



def formatUnit(){
	if(unitFormat == "Imperial"){
		state.unit = "F"
        if(logSet == true){log.info "state.unit = $state.unit"}
	}
	if(unitFormat == "Metric"){
		state.unit = "C"
        if(logSet == true){log.info "state.unit = $state.unit"}
	}
}

def ForcePoll(){
    if(logSet == true){log.debug "PC: Poll called"}
    state.NumOfPolls = (state.NumOfPolls) + 1
	poll1()	
}
	
def pollHandler1(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		data = parseJson(resp.data)
        if(logSet == true){log.debug "Response Data1 = $data"}		// log the data returned by pool controller
        if(logSet == false){log.info "Further logging disabled"}
            sendEvent(name: "pollsSinceReset", value: state.NumOfPolls)
            sendEvent(name: "poolTemp", value: data.temperature.poolTemp, unit: ${$state.unit})
			sendEvent(name: "airTemp", value: data.temperature.airTemp, unit: ${$state.unit})
		state.lastPoll = now()

	} else {
        def res = resp.getStatus()
		log.error "Pool Controller API did not return data from poll1 - $res"
	}
}        	
	
	
def poll1(){
    formatUnit()
    def params1 = [
        uri: "http://${serverAddress}:${serverPort}/all"
     //   uri: "https://pool.rainseed.net:3000/v2/pws/observations/current?stationId=IDURHAM16&format=json&units=${state.unit}&apiKey=${apiKey}"
    ]
    asynchttpGet("pollHandler1", params1)   
}

def logsOff() {
log.warn "Debug logging disabled..."
device.updateSetting("logSet", [value: "false", type: "bool"])}
