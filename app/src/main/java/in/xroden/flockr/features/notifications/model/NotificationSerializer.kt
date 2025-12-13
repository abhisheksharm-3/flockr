package `in`.xroden.flockr.features.notifications.model

import `in`.xroden.flockr.data.enums.NotificationType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object NotificationSerializer : JsonTransformingSerializer<Notification>(Notification.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        
        val typePrimitive = element["type"] as? JsonPrimitive
        val typeString = typePrimitive?.content ?: return element
        
        if (typeString.startsWith("house_invitation")) {
            val newElement = element.toMutableMap()
            
            // Fix Type
            newElement["type"] = JsonPrimitive("house_invitation")
            
            // Extract Code if present (e.g. house_invitation:CODE)
            if (typeString.contains(":")) {
                val code = typeString.substringAfter(":")
                
                // Construct Data payload
                val existingData = element["data"]
                val newData = if (existingData is JsonPrimitive && existingData.isString) {
                    // Start with existing JSON string if possible, but simplest is to just overwrite
                    // or merge. Since data is a string-encoded JSON usually.
                    // Let's create a new JSON object for data.
                    try {
                        val currentJson = Json.parseToJsonElement(existingData.content) as? JsonObject
                        val merged = currentJson?.toMutableMap() ?: mutableMapOf()
                        merged["invite_code"] = JsonPrimitive(code)
                        JsonObject(merged).toString()
                    } catch (e: Exception) {
                        """{"invite_code":"$code"}"""
                    }
                } else {
                     """{"invite_code":"$code"}"""
                }
                
                newElement["data"] = JsonPrimitive(newData)
            }
            
            return JsonObject(newElement)
        }
        
        return element
    }
}
