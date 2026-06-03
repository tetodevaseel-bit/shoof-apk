import { TelegramClient } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';

const API_ID = 11704522;
const API_HASH = '4d8519ffb0302f8ac42574a44839bde3';

let client = null;
let resolveCode = null;
let resolvePassword = null;

export function getClient() {
    return client;
}

export async function initClient(savedSession = '') {
    const session = new StringSession(savedSession);
    client = new TelegramClient(session, API_ID, API_HASH, {
        connectionRetries: 5,
    });
    await client.connect();
    return client;
}

export async function sendCode(phone) {
    if (!client) await initClient();
    const result = await client.sendCode({ apiId: API_ID, apiHash: API_HASH }, phone);
    return result;
}

export async function signIn(phone, phoneCodeHash, code) {
    return await client.invoke(
        new (await import('telegram/tl/functions/auth/index.js')).SignIn({
            phoneNumber: phone,
            phoneCodeHash,
            phoneCode: code,
        })
    );
}

export async function startAuth({ phone, onCodeRequired, onPasswordRequired }) {
    if (!client) await initClient();

    await client.start({
        phoneNumber: async () => phone,
        phoneCode: async () => {
            return new Promise((resolve) => {
                resolveCode = resolve;
            });
        },
        password: async () => {
            return new Promise((resolve) => {
                resolvePassword = resolve;
            });
        },
        onError: (err) => { throw err; },
    });

    return client.session.save();
}

export function submitCode(code) {
    if (resolveCode) {
        resolveCode(code);
        resolveCode = null;
    }
}

export function submitPassword(password) {
    if (resolvePassword) {
        resolvePassword(password);
        resolvePassword = null;
    }
}

export function getSession() {
    return client?.session?.save?.() ?? '';
}
