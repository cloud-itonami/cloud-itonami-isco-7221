# Security Policy

This project handles forge-shop operating workflows. Treat vulnerabilities
as potentially high impact even when the demo data is synthetic — this
domain's failure modes include physical worker-safety risk (heat exposure,
press-crush hazard, hot-metal handling).

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real worker, forge-shop or operator data exposure
- authorization bypass
- Forge Worker Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach a forging-execution decision, or
  a forge-shop-safety-officer-override decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on worker/forge-shop data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real worker/forge-shop/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
