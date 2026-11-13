# MIKROTECK 301 PRO - LAB ONLY, one-shot RouterOS 7 agent
# Execute via /import file-name=agent-v7.rsc, NOT line-by-line terminal paste.
# Requires global mkRemoteBase, mkRemoteRouterId, mkRemoteToken already configured.
# No scheduler; only router_status read-only requests.
:global mkRemoteBase
:global mkRemoteRouterId
:global mkRemoteToken
:if (([:typeof $mkRemoteBase] != "str") || ([:typeof $mkRemoteRouterId] != "str") || ([:typeof $mkRemoteToken] != "str")) do={ :error "MIKROTECK: missing globals" }
:if (([:pick $mkRemoteBase 0 8] != "https://") || ([:len $mkRemoteRouterId] < 8) || ([:len $mkRemoteToken] < 32)) do={ :error "MIKROTECK: invalid globals" }
:local base ($mkRemoteBase . "/v1/routers/" . $mkRemoteRouterId)
:local headers ("Authorization: Bearer " . $mkRemoteToken . ",Accept: application/json")
:local response [/tool fetch url=($base . "/next") http-method=get http-header-field=$headers check-certificate=yes output=user as-value]
:if (($response->"status") != "finished") do={ :error "MIKROTECK: poll failed" }
:local parsed [:deserialize from=json value=($response->"data")]
:local job ($parsed->"job")
:if ([:typeof $job] != "array") do={ :put "MIKROTECK: no pending job"; :return }
:local jobId ($job->"id")
:local action ($job->"action")
:if (([:typeof $jobId] != "str") || ([:len $jobId] != 36)) do={ :error "MIKROTECK: invalid job id" }
:if ($action != "router_status") do={ :error "MIKROTECK: action denied" }
:local idJson [:serialize to=json value=$jobId options=json.no-string-conversion]
:local uptimeJson [:serialize to=json value=[:tostr [/system resource get uptime]] options=json.no-string-conversion]
:local versionJson [:serialize to=json value=[:tostr [/system resource get version]] options=json.no-string-conversion]
:local payload ("{\"id\":" . $idJson . ",\"result\":{\"status\":\"online\",\"uptime\":" . $uptimeJson . ",\"routerosVersion\":" . $versionJson . "}}")
:local upload [/tool fetch url=($base . "/result") http-method=post http-header-field=($headers . ",Content-Type: application/json") http-data=$payload check-certificate=yes output=user as-value]
:if (($upload->"status") != "finished") do={ :error "MIKROTECK: upload failed" }
:put ($upload->"data")
