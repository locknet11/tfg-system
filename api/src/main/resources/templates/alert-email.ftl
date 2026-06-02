Security Alert

Event Type: ${eventType}
Timestamp: ${timestamp}
<#if severity??>
Severity: ${severity}
</#if>
<#if payloadEntries?has_content>

Details:
<#list payloadEntries as entry>
  ${entry.key}: ${entry.value}
</#list>
</#if>

---
This is an automated alert from the TFG Security System.
