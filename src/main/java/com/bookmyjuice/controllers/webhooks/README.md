# Webhooks Architecture

## Overview
This directory contains specific webhook handlers for BookMyJuice. The monolithic `WebhookController` has been decomposed into smaller, focused controllers to improve maintainability and reduce collision risks.

## Structure
- **Base Path:** `/api/webhooks`
- **Controllers:** Each resource (e.g., `customers`, `subscriptions`) has its own dedicated controller (e.g., `CustomerWebhookController`, `SubscriptionWebhookController`).
- **Idempotency:** Handled by `IdempotencyService` to prevent duplicate processing of Chargebee events.
- **Processing:** Complex logic is delegated to `WebhookEventProcessor`.

## Security
All endpoints in this package are secured by Basic Authentication defined in `application.properties` (`webhook.username` and `webhook.password`).

## Removed Dead Code
- `WebhookController.java` (Legacy monolith, fully commented out)
- `WebhookControllerBackup.java` (Unused backup)
