# MIKROTECK — one-shot authentication probe (RouterOS 7)
# LAB ONLY. No scheduler, no persistent configuration, no router changes.
# Prerequisites: define mkRemoteToken locally, never paste it in chat.
# :global mkRemoteToken "YOUR_OWN_ROUTER_TOKEN"
# Run by pasting this script in WinBox terminal only after review.
:global mkRemoteToken
:if ([:typeof $mkRemoteToken] != "str") do={ :error "Missing mkRemoteToken" }
:if ([:len $mkRemoteToken] < 32) do={ :error "Router token too short" }
:local url "https://mikroteck-remote-relay.je0515356.workers.dev/v1/routers/router-test-0001/next"
:local headers ("Authorization: Bearer " . $mkRemoteToken . ",Accept: application/json")
:local result
:onerror err in={
    :set result [/tool fetch url=$url http-method=get http-header-field=$headers check-certificate=yes output=user as-value]
} do={
    :error "HTTPS request failed; check token and Cloudflare configuration"
}
:if (($result->"status") != "finished") do={ :error "HTTPS request did not finish" }
:put ("HTTP response: " . ($result->"data"))
# Expected when no queued job: {"job":null}
# This does not prove that Android remote control works.
