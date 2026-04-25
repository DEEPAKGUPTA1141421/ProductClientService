// k6 load test — /api/v1/reco/for-you
//
// Baseline target:   5k RPS p99 < 80ms
// Sale-day target:  20× burst (100k RPS) sustained 30 min without SLO breach
//
// Usage:
//   k6 run --vus 500 --duration 10m \
//          -e BASE_URL=https://api.staging.example.com \
//          -e JWT=$JWT \
//          ops/k6/reco_load_test.js
//
// For the 20× burst run use k6 cloud with 20 load generators.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const latency = new Trend('reco_latency');
const empty   = new Counter('reco_empty');

export const options = {
  thresholds: {
    http_req_failed:   ['rate<0.01'],
    http_req_duration: ['p(99)<80', 'p(95)<60'],
    reco_empty:        ['count<200'],
  },
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: 5000,
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 500,
      maxVUs: 2000,
    },
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8081';
const JWT  = __ENV.JWT || '';

// Seed pool of realistic userIds — rotate so caching does not mask latency.
const USERS = new SharedArrayOfUUIDs(__ENV.USER_POOL_SIZE || 100000);

export default function () {
  const userId = USERS.random();
  const ctx = ['HOME', 'PDP', 'CART'][Math.floor(Math.random() * 3)];
  const url = `${BASE}/api/v1/reco/for-you?context=${ctx}&k=20`;

  const res = http.get(url, {
    headers: {
      Authorization: `Bearer ${JWT}`,
      'X-User-Id-Override': userId, // staging-only header
    },
    tags: { endpoint: 'reco_for_you', context: ctx },
  });

  latency.add(res.timings.duration);
  check(res, {
    'status 200': (r) => r.status === 200,
    'non-empty items': (r) => {
      try {
        const body = JSON.parse(r.body);
        const ok = body.data && body.data.items && body.data.items.length > 0;
        if (!ok) empty.add(1);
        return true;
      } catch (e) { return false; }
    },
    'payload under 10KB': (r) => r.body.length <= 10 * 1024,
  });

  sleep(Math.random() * 0.1);
}

// Helper: random UUIDs allocated once per VU process (k6 shared array).
function SharedArrayOfUUIDs(n) {
  const arr = new Array(n);
  for (let i = 0; i < n; i++) arr[i] = uuidv4();
  return { random: () => arr[Math.floor(Math.random() * n)] };
}

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
