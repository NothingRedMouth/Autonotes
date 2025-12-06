import http from 'k6/http';
import { check, sleep, fail } from 'k6';

const imageFile = open('../data/sample-note.jpg', 'b');

const JWT_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZXd1c2VyIiwiaWF0IjoxNzY0OTc3MDM2LCJleHAiOjE3NjUwNjM0MzZ9.MdwiK9IMvkLe-DoG9YAy1zBPlb7cLCkacAqtwFz8mK0';

if (JWT_TOKEN.startsWith('eyJhbGci')) {
} else {
    fail('FATAL: JWT_TOKEN is not set. Please paste your token into the script.');
}

export const options = {
    stages: [
        { duration: '20s', target: 10 },

        { duration: '40s', target: 10 },

        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration': ['p(95)<500'],
    },
};

export default function () {
    const data = {
        title: `k6 Note ${__VU}-${__ITER}`,
        file: http.file(imageFile, `test-image-${__VU}-${__ITER}.jpg`, 'image/jpeg'),
    };

    const params = {
        headers: {
            'Authorization': `Bearer ${JWT_TOKEN}`,
        },
    };

    const res = http.post('http://localhost:8080/api/v1/notes', data, params);

    check(res, {
        'API returned 201 Created': (r) => r.status === 201,
        'Response contains note ID': (r) => typeof r.json('id') === 'number',
    });
}
