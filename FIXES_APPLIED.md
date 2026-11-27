# FoodExpress – Source Review and Fix Report

## Changes made

### Backend reliability and data handling
- Added transaction boundaries to service read methods that map lazy JPA relationships. This prevents detached-entity / `LazyInitializationException` failures in restaurant, menu, order, payment, review, cart, and delivery responses.
- Added server-side cart quantity limits and changed the cart badge count to represent total item quantity rather than the number of distinct lines.
- Revalidates food availability, category activity, restaurant state, and current database prices during checkout.
- Prevents adding items from closed, inactive, or unapproved restaurants.
- Prevents assigning a food item to a category belonging to another restaurant or to an inactive category.
- Added owner-only menu endpoints so restaurant owners can see unavailable items and inactive categories and manage their existing menu instead of having those entries disappear from their dashboard.
- Changed food-item deletion to deactivate the item, preserving historical order-item foreign-key references.
- Prevents deleting delivery addresses already referenced by orders.
- Validates coupon date ranges and percentage values, and lists only coupons currently within their validity window.
- Improved validation-error handling for requests that contain global (non-field) validation errors.

### Order, delivery, and payment workflow
- Enforced valid restaurant order-status transitions.
- Prevents confirming unpaid online orders and prevents rejecting an already-paid order without refund handling.
- Prevents customers from self-cancelling an already-paid order when no refund workflow is configured.
- Delivery assignment now requires an order to be ready for pickup and, for online orders, payment to be confirmed.
- Delivery assignment no longer marks an order as picked up before the partner physically picks it up.
- Delivery partner status transitions now update the corresponding order status and order-status history.
- Delivery partners are reserved while an assignment is active and become available again after delivery or a failed attempt.
- Failed delivery assignments can be reassigned by an administrator.
- Added an availability endpoint and synchronized the delivery dashboard badge with the server state.
- Payment verification is idempotent for a repeated successful callback, uses constant-time signature comparison, and persists a failed-verification status instead of rolling it back with the error response.
- Allows a customer to retry payment after a previous verification failure and blocks payment for cancelled or rejected orders.
- Uses exact paise conversion for Razorpay amounts instead of silently truncating values.
- Removed an unused frontend payment implementation containing a hard-coded Razorpay placeholder key; checkout is handled through the payment page and receives the key ID from the backend.

### Documentation
- Rewrote the README with Windows, macOS/Linux, and Docker startup instructions, demo accounts, payment-mode details, and configuration guidance.

## Checks performed

- `frontend/js/app.js`: Node.js syntax check passed.
- Inline JavaScript: all 13 inline script blocks passed `node --check`.
- Python frontend server: `py_compile` passed.
- `backend/pom.xml`: valid XML.
- `docker-compose.yml`: valid YAML; services parsed as mysql, backend, frontend.
- Local HTML assets: all 26 checked local script, stylesheet, and image references exist.
- Frontend static server: homepage and shared JavaScript returned HTTP 200.
- Java source parser pass: no Java syntax diagnostics detected.

## Not verified in this environment

A full Maven compile/package, Spring Boot startup, Flyway execution against MySQL, and live Razorpay transaction could not be run here because Maven, Docker, and MySQL are not installed in the execution environment, and external dependency downloads are unavailable. The Java source therefore still needs a real Maven build on a machine with dependency access and Java 21.

The default `PAYMENT_DEMO=true` mode is a simulated local checkout. Actual Razorpay Test Mode requires your own Razorpay test key ID and secret with `PAYMENT_DEMO=false`.
