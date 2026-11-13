# MIKROTECK remote agent V1 — LAB ONLY, RouterOS 7.15+ recommended.
# Read-only, one-shot status response. No scheduler or configuration changes.
# Tested protocol manually on hAP ac2 RouterOS 7.24.6; this full script is NOT hardware-validated.
# Set these three globals manually in a lab terminal BEFORE running:
# :global mkRemoteBase "https://YOUR-WORKER.workers.dev"
# :global mkRemoteRouterId "router-test-0001"
# :global mkRemoteToken "TEST-ONLY-TOKEN"
# Do not share/export a backup containing these secrets.
:global mkRemoteBase
:global mkRemoteRouterId
:global mkRemoteToken
:if (([:typeof $mkRemoteBase] != "str") || ([:typeof $mkRemoteRouterId] != "str") || ([:typeof $mkRemoteToken] != "str")) do={
    :error "MIKROTECK: missing configuration"
}
:if (([:pick $mkRemoteBase 0 8] != "https://") || ([:len $mkRemoteRouterId] < 8) || ([:len $mkRemoteToken] < 16)) do={
    :error "MIKROTECK: invalid configuration"
}
:local base ($mkRemoteBase . "/v1/routers/" . $mkRemoteRouterId)
:local headers ("Authorization: Bearer " . $mkRemoteToken . ",Accept: application/json")
:local response
:onerror err in={
    :set response [/tool fetch url=($base . "/next") http-method=get http-header-field=$headers check-certificate=yes output=user as-value]
} do={
    :log warning "MIKROTECK: remote poll failed (network/TLS/auth)"
    :error "MIKROTECK: poll failed"
}
:if (($response->"status") != "finished") do={ :error "MIKROTECK: incomplete response" }
:local parsed
:onerror err in={ :set parsed [:deserialize from=json value=($response->"data")] } do={
    :error "MIKROTECK: malformed JSON"
}
:local job ($parsed->"job")
:if ([:typeof $job] != "array") do={ :return }
:local jobId ($job->"id")
:local action ($job->"action")
:if (([:typeof $jobId] != "str") || ([:len $jobId] != 36)) do={
    :error "MIKROTECK: invalid job id"
}
# Intentionally reject all other commands until per-router auth is deployed.
:if ($action != "router_status") do={
    :log warning "MIKROTECK: unsupported action ignored"
    :return
}
:local idJson [:serialize to=json value=$jobId options=json.no-string-conversion]
:local uptimeJson [:serialize to=json value=[:tostr [/system resource get uptime]] options=json.no-string-conversion]
:local versionJson [:serialize to=json value=[:tostr [/system resource get version]] options=json.no-string-conversion]
:local payload ("{\"id\":" . $idJson . ",\"result\":{\"status\":\"online\",\"uptime\":" . $uptimeJson . ",\"routerosVersion\":" . $versionJson . "}}")
:onerror err in={
    :local upload [/tool fetch url=($base . "/result") http-method=post http-header-field=($headers . ",Content-Type: application/json") http-data=$payload check-certificate=yes output=user as-value]
    :if (($upload->"status") != "finished") do={ :error "MIKROTECK: upload incomplete" }
    :put ($upload->"data")
} do={
    :log warning "MIKROTECK: result upload failed"
    :error "MIKROTECK: upload failed"
}
