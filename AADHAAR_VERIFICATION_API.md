# Aadhaar Verification API Documentation

## Overview

The Aadhaar Verification API allows sellers to verify their Aadhaar number by sending an OTP to their registered mobile number and then verifying that OTP. This is a two-step process:

1. **Send OTP** - Trigger Aadhaar verification and send OTP to mobile
2. **Verify OTP** - Verify the OTP received on mobile

## API Endpoints

### 1. Send Aadhaar OTP

**Endpoint:** `POST /api/v1/seller/product/aadhaar/send-otp`

**Authentication:** Required (SELLER role)

**Request Body:**
```json
{
  "aadhaarNumber": "123456789012",
  "phoneNumber": "9876543210"
}
```

**Request Parameters:**
- `aadhaarNumber` (string, required): 12-digit Aadhaar number
- `phoneNumber` (string, required): 10-digit mobile number

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "OTP sent successfully to your registered mobile number",
  "data": "550e8400-e29b-41d4-a716-446655440000",
  "statusCode": 200
}
```

**Response (Failure - 400/500):**
```json
{
  "success": false,
  "message": "Invalid Aadhaar number or phone mismatch",
  "data": null,
  "statusCode": 400
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/seller/product/aadhaar/send-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "aadhaarNumber": "123456789012",
    "phoneNumber": "9876543210"
  }'
```

---

### 2. Verify Aadhaar OTP

**Endpoint:** `POST /api/v1/seller/product/aadhaar/verify-otp`

**Authentication:** Required (SELLER role)

**Query Parameters:**
- `phoneNumber` (string, required): 10-digit mobile number
- `otp` (string, required): 6-digit OTP received on mobile

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "Aadhaar verification completed successfully",
  "data": "9876543210",
  "statusCode": 200
}
```

**Response (Failure - 400):**
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "data": null,
  "statusCode": 400
}
```

**Example cURL:**
```bash
curl -X POST "http://localhost:8081/api/v1/seller/product/aadhaar/verify-otp?phoneNumber=9876543210&otp=123456" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Implementation Details

### Service: AadhaarVerificationService

The `AadhaarVerificationService` handles:

1. **Aadhaar Validation** - Calls external Aadhaar API to validate the Aadhaar number against the phone number
2. **OTP Generation** - Generates a random 6-digit OTP
3. **OTP Storage** - Stores the OTP in the database with a 5-minute expiry
4. **SMS Delivery** - Sends the OTP to the provided phone number via SMS API
5. **OTP Verification** - Validates the provided OTP against the stored OTP

### Database Model: Otp

The OTP record is stored with the following fields:
- `id` - UUID identifier
- `phone` - Phone number (10 digits)
- `otpCode` - 6-digit OTP code
- `isVerified` - Boolean flag indicating verification status
- `expiryTime` - Expiry timestamp (default: 5 minutes from creation)
- `type` - Type of OTP (e.g., "aadhaarVerification")
- `createdAt` - Creation timestamp
- `updatedAt` - Last update timestamp

### Configuration

Add the following properties to `application.properties` or environment variables:

```properties
# Aadhaar Verification API
aadhaar.api.endpoint=${AADHAAR_API_ENDPOINT}
aadhaar.api.key=${AADHAAR_API_KEY}

# SMS Service API
sms.api.endpoint=${SMS_API_ENDPOINT}
sms.api.key=${SMS_API_KEY}
```

**Example Environment Variables:**
```
AADHAAR_API_ENDPOINT=https://api.aadhaarverification.com/verify
AADHAAR_API_KEY=your_aadhaar_api_key
SMS_API_ENDPOINT=https://api.sms-provider.com/send
SMS_API_KEY=your_sms_api_key
```

---

## External API Integration

### Aadhaar Verification API

The service calls an external Aadhaar verification API with the following request format:

**Request:**
```json
{
  "aadhaarNumber": "123456789012",
  "phoneNumber": "9876543210"
}
```

**Expected Response:**
```json
{
  "valid": true,
  "phoneMatches": true,
  "name": "John Doe",
  "aadhaarNumber": "123456789012"
}
```

### SMS Service API

The service calls an SMS API to send OTP with the following request format:

**Request:**
```json
{
  "phone": "9876543210",
  "message": "Your Aadhaar verification OTP is: 123456. Valid for 5 minutes."
}
```

---

## Flow Diagram

```
Seller
  |
  ├─> Send Aadhaar OTP
  │    └─> Validate Aadhaar (External API)
  │    └─> Generate OTP
  │    └─> Store OTP in Database
  │    └─> Send SMS (External API)
  │    └─> Return OTP ID
  │
  ├─> Receive OTP on Mobile
  │
  └─> Verify OTP
       └─> Validate OTP (Check Database)
       └─> Mark as Verified
       └─> Return Success
```

---

## Error Handling

### Common Error Codes

| Status Code | Error Message | Solution |
|------------|---------------|----------|
| 400 | Invalid Aadhaar number or phone mismatch | Verify that Aadhaar and phone number are correct |
| 400 | Invalid or expired OTP | Re-request OTP or use the new OTP |
| 500 | Failed to send OTP via SMS | Check SMS API configuration |
| 500 | Error processing Aadhaar verification | Check Aadhaar API configuration |

---

## Security Considerations

1. **JWT Authentication** - All endpoints require valid JWT token with SELLER role
2. **OTP Expiry** - OTP expires after 5 minutes
3. **Phone Number Masking** - Phone numbers are masked in logs for privacy
4. **API Key Protection** - API keys should be stored securely as environment variables
5. **Rate Limiting** - Consider implementing rate limiting to prevent abuse

---

## Testing

### Test Scenarios

1. **Valid Aadhaar and Phone**
   - Send OTP with valid credentials
   - Verify OTP with correct code
   - Expected: Success

2. **Invalid Aadhaar**
   - Send OTP with invalid Aadhaar
   - Expected: Error 400 - Invalid Aadhaar

3. **Expired OTP**
   - Wait more than 5 minutes after sending OTP
   - Try to verify
   - Expected: Error 400 - Expired OTP

4. **Wrong OTP**
   - Send OTP with wrong code
   - Expected: Error 400 - Invalid OTP

---

## Logging

The service includes comprehensive logging at various levels:

- **INFO**: OTP generation, SMS sending, OTP verification
- **WARN**: Invalid OTP, failed API calls
- **ERROR**: Exception handling, API failures

Check logs with:
```bash
grep "AadhaarVerificationService" logs/application.log
```

---

## Future Enhancements

1. Add support for other verification methods (PAN, DL)
2. Implement rate limiting per phone number
3. Add SMS template customization
4. Support for multiple OTP resend attempts
5. Webhook integration for external systems
6. Analytics and audit trail
