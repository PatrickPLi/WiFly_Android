package com.example.joystick

// Solace PubSub+ Broker Options

// Fill in your Solace Cloud PubSub+ Broker's 'MQTT Host' and 'Password' options.

// This information can be found under:

// https://console.solace.cloud/services/ -> <your-service> -> 'Connect' -> 'MQTT'

const val SOLACE_CLIENT_USER_NAME = "solace-cloud-client"

const val SOLACE_CLIENT_PASSWORD = "p2i7li6ckbaimoe0draq0qdl82"

const val SOLACE_MQTT_HOST = "tcp://mrzpfs1b9tj1n.messaging.solace.cloud:20550"


// Other options

const val SOLACE_CONNECTION_TIMEOUT = 3

const val SOLACE_CONNECTION_KEEP_ALIVE_INTERVAL = 60

const val SOLACE_CONNECTION_CLEAN_SESSION = true

const val SOLACE_CONNECTION_RECONNECT = true
