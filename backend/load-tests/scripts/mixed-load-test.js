import http from 'k6/http';
import { check, sleep, fail } from 'k6';

const imageFile = open('../data/sample-note.jpg', 'b');
const JWT_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZXd1c2VyIiwiaWF0IjoxNzY0OTgwNzg4LCJleHAiOjE3NjUwNjcxODh9.f9gc9B9Q6_nV6tKh4k5dvT-Z-gW_kjccMGQsmmO5Aqg';

if (!JWT_TOKEN.startsWith('ey')) {
    fail('FATAL: JWT_TOKEN is not set. Please paste your token into the script.');
}

let createdNoteIds = [];

export const options = {
    scenarios: {
        upload_and_read: {
            executor: 'ramping-vus',
            exec: 'uploadAndRead',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 15 },
                { duration: '1m', target: 15 },
                { duration: '10s', target: 0 },
            ],
        },
        only_read: {
            executor: 'constant-vus',
            exec: 'onlyRead',
            vus: 10,
            duration: '1m40s',
        },
    },
    thresholds: {
        'http_req_duration{scenario:upload_and_read}': ['p(95)<1500'],
        'http_req_duration{scenario:only_read}': ['p(95)<200'],
        'http_req_failed': ['rate<0.02'],
    },
};

export function uploadAndRead() {
    const params = { headers: { 'Authorization': `Bearer ${JWT_TOKEN}` } };

    const uploadData = {
        title: `Mixed Load Note ${__VU}-${__ITER}`,
        file: http.file(imageFile, `test-${__VU}-${__ITER}.jpg`, 'image/jpeg'),
    };

    const uploadRes = http.post('http://localhost:8080/api/v1/notes', uploadData, params);

    check(uploadRes, { 'Upload status 201': (r) => r.status === 201 });

    if (uploadRes.status === 201) {
        createdNoteIds.push(uploadRes.json('id'));
    }

    sleep(Math.random() * 3 + 1);

    if (Math.random() < 0.3 && createdNoteIds.length > 0) {
        const noteIdToRead = createdNoteIds[Math.floor(Math.random() * createdNoteIds.length)];

        const detailRes = http.get(`http://localhost:8080/api/v1/notes/${noteIdToRead}`, params);
        check(detailRes, { 'Read Detail status 200': (r) => r.status === 200 });
        sleep(1);
    }

    if (Math.random() < 0.1 && createdNoteIds.length > 5) {
        const noteIdToDelete = createdNoteIds.shift();

        const deleteRes = http.del(`http://localhost:8080/api/v1/notes/${noteIdToDelete}`, null, params);
        check(deleteRes, { 'Delete status 204': (r) => r.status === 204 });
        sleep(1);
    }
}

export function onlyRead() {
    const params = { headers: { 'Authorization': `Bearer ${JWT_TOKEN}` } };

    const listRes = http.get('http://localhost:8080/api/v1/notes', params);

    check(listRes, { 'Read List status 200': (r) => r.status === 200 });

    sleep(Math.random() * 5 + 3);
}
