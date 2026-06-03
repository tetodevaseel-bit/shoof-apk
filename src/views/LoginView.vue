<script setup>
import { ref } from 'vue';
import { TelegramClient } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';

const emit = defineEmits(['logged-in']);

const API_ID = 11704522;
const API_HASH = '4d8519ffb0302f8ac42574a44839bde3';

const step = ref('welcome');
const phone = ref('');
const code = ref(['', '', '', '', '']);
const password = ref('');
const error = ref('');
const loading = ref(false);
const selectedCountry = ref({ code: '+966', name: 'السعودية', flag: '🇸🇦' });
const showCountry = ref(false);
const codeInputs = ref([]);

let client = null;
let resolveCode = null;
let resolvePassword = null;

const countries = [
    { code: '+966', name: 'السعودية',     flag: '🇸🇦' },
    { code: '+971', name: 'الإمارات',     flag: '🇦🇪' },
    { code: '+965', name: 'الكويت',       flag: '🇰🇼' },
    { code: '+974', name: 'قطر',          flag: '🇶🇦' },
    { code: '+973', name: 'البحرين',      flag: '🇧🇭' },
    { code: '+968', name: 'عُمان',        flag: '🇴🇲' },
    { code: '+962', name: 'الأردن',       flag: '🇯🇴' },
    { code: '+961', name: 'لبنان',        flag: '🇱🇧' },
    { code: '+964', name: 'العراق',       flag: '🇮🇶' },
    { code: '+963', name: 'سوريا',        flag: '🇸🇾' },
    { code: '+967', name: 'اليمن',        flag: '🇾🇪' },
    { code: '+20',  name: 'مصر',          flag: '🇪🇬' },
    { code: '+218', name: 'ليبيا',        flag: '🇱🇾' },
    { code: '+213', name: 'الجزائر',      flag: '🇩🇿' },
    { code: '+216', name: 'تونس',         flag: '🇹🇳' },
    { code: '+212', name: 'المغرب',       flag: '🇲🇦' },
    { code: '+249', name: 'السودان',      flag: '🇸🇩' },
    { code: '+1',   name: 'أمريكا',       flag: '🇺🇸' },
    { code: '+44',  name: 'بريطانيا',     flag: '🇬🇧' },
    { code: '+90',  name: 'تركيا',        flag: '🇹🇷' },
    { code: '+7',   name: 'روسيا',        flag: '🇷🇺' },
    { code: '+49',  name: 'ألمانيا',      flag: '🇩🇪' },
    { code: '+33',  name: 'فرنسا',        flag: '🇫🇷' },
    { code: '+91',  name: 'الهند',        flag: '🇮🇳' },
    { code: '+92',  name: 'باكستان',      flag: '🇵🇰' },
    { code: '+62',  name: 'إندونيسيا',   flag: '🇮🇩' },
    { code: '+86',  name: 'الصين',        flag: '🇨🇳' },
    { code: '+81',  name: 'اليابان',      flag: '🇯🇵' },
    { code: '+55',  name: 'البرازيل',     flag: '🇧🇷' },
    { code: '+61',  name: 'أستراليا',     flag: '🇦🇺' },
    { code: '+98',  name: 'إيران',        flag: '🇮🇷' },
];

async function startLogin() {
    if (!phone.value.trim() || loading.value) return;
    loading.value = true;
    error.value = '';

    const fullPhone = selectedCountry.value.code + phone.value.trim();

    try {
        client = new TelegramClient(new StringSession(''), API_ID, API_HASH, {
            connectionRetries: 5,
        });
        await client.connect();

        client.start({
            phoneNumber: async () => fullPhone,
            phoneCode: async () => {
                step.value = 'code';
                loading.value = false;
                return new Promise(r => { resolveCode = r; });
            },
            password: async () => {
                step.value = 'password';
                loading.value = false;
                return new Promise(r => { resolvePassword = r; });
            },
            onError: (err) => { throw err; },
        }).then(async () => {
            step.value = 'loading';
            const session = client.session.save();
            try {
                const { Preferences } = await import('@capacitor/preferences');
                await Preferences.set({ key: 'tg_session', value: session });
            } catch {
                localStorage.setItem('tg_session', session);
            }
            const me = await client.getMe();
            emit('logged-in', { session, userId: me.id?.toString(), phone: fullPhone });
        }).catch(err => {
            error.value = err.message || 'خطأ في تسجيل الدخول';
            loading.value = false;
            step.value = 'phone';
        });

    } catch (err) {
        error.value = err.message || 'تعذّر الاتصال بتيليجرام';
        loading.value = false;
    }
}

function onCodeInput(i, e) {
    const val = e.target.value.replace(/\D/g, '').slice(-1);
    code.value[i] = val;
    if (val && i < 4) codeInputs.value[i + 1]?.focus();
    if (code.value.join('').length === 5) submitCode();
}

function onCodeKeydown(i, e) {
    if (e.key === 'Backspace' && !code.value[i] && i > 0) {
        codeInputs.value[i - 1]?.focus();
    }
}

function submitCode() {
    const c = code.value.join('');
    if (c.length !== 5 || !resolveCode) return;
    loading.value = true;
    resolveCode(c);
    resolveCode = null;
}

function submitPassword() {
    if (!password.value.trim() || !resolvePassword) return;
    loading.value = true;
    resolvePassword(password.value.trim());
    resolvePassword = null;
}
</script>

<template>
<div dir="rtl" class="relative min-h-screen bg-black text-white overflow-hidden flex flex-col select-none">

    <div class="absolute inset-0 bg-gradient-to-b from-purple-950/40 via-black to-black pointer-events-none"></div>
    <div class="absolute top-0 inset-x-0 h-64 bg-gradient-to-b from-red-950/20 to-transparent pointer-events-none"></div>

    <!-- Loading -->
    <div v-if="step === 'loading'" class="relative z-10 flex-1 flex flex-col items-center justify-center gap-4">
        <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-red-600 to-red-800 flex items-center justify-center text-4xl">🎬</div>
        <div class="flex gap-1.5 mt-2">
            <div v-for="i in 3" :key="i" class="w-2 h-2 rounded-full bg-red-500 animate-bounce" :style="`animation-delay:${(i-1)*0.15}s`"></div>
        </div>
        <p class="text-gray-400 text-sm">جاري الدخول...</p>
    </div>

    <!-- Welcome -->
    <div v-else-if="step === 'welcome'" class="relative z-10 flex flex-col flex-1">
        <div class="flex-1 flex flex-col items-center justify-center px-8 gap-6 pt-16">
            <div class="relative">
                <div class="w-32 h-32 rounded-3xl bg-gradient-to-br from-red-600 via-red-700 to-red-900 flex items-center justify-center shadow-2xl shadow-red-900/60">
                    <span class="text-6xl">🎬</span>
                </div>
                <div class="absolute -bottom-2 -left-2 bg-green-500 w-8 h-8 rounded-full flex items-center justify-center border-2 border-black text-sm font-bold">▶</div>
            </div>
            <div class="text-center">
                <h1 class="text-4xl font-black tracking-tight">شوف <span class="text-red-500">TV</span></h1>
                <p class="text-gray-400 mt-2 text-sm leading-relaxed">شاهد آلاف المسلسلات والأنيميات<br/>بجودة عالية في أي مكان</p>
            </div>
        </div>
        <div class="px-6 pb-12 space-y-3">
            <button @click="step='phone'" class="w-full py-4 rounded-2xl bg-red-600 font-bold text-lg active:scale-95 transition-transform shadow-lg shadow-red-900/40">
                ابدأ المشاهدة مجاناً
            </button>
            <p class="text-center text-gray-600 text-xs">يتطلب حساب تيليجرام</p>
        </div>
    </div>

    <!-- Phone -->
    <div v-else-if="step === 'phone'" class="relative z-10 flex flex-col flex-1 px-6">
        <div class="pt-12 pb-6">
            <button @click="step='welcome'" class="text-gray-500 text-sm flex items-center gap-1 mb-6">← رجوع</button>
            <h2 class="text-3xl font-black">أدخل رقمك</h2>
            <p class="text-gray-500 mt-1 text-sm">سيصلك رمز التحقق على تيليجرام مباشرة</p>
        </div>
        <div class="space-y-3 flex-1">
            <button @click="showCountry=!showCountry"
                class="w-full bg-white/8 border border-white/12 rounded-2xl px-4 py-3.5 flex items-center gap-3">
                <span class="text-2xl">{{ selectedCountry.flag }}</span>
                <div class="flex-1 text-right">
                    <div class="text-white text-sm font-medium">{{ selectedCountry.name }}</div>
                    <div class="text-gray-500 text-xs">{{ selectedCountry.code }}</div>
                </div>
                <span class="text-gray-600 text-xs">{{ showCountry ? '▲' : '▼' }}</span>
            </button>
            <div v-if="showCountry" class="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden max-h-52 overflow-y-auto">
                <button v-for="c in countries" :key="c.code" @click="selectedCountry=c; showCountry=false"
                    class="w-full px-4 py-3 flex items-center gap-3 active:bg-white/8 border-b border-white/5 last:border-0">
                    <span class="text-xl">{{ c.flag }}</span>
                    <span class="flex-1 text-sm text-right text-white">{{ c.name }}</span>
                    <span class="text-gray-500 text-xs font-mono">{{ c.code }}</span>
                </button>
            </div>
            <div class="bg-white/8 border border-white/12 rounded-2xl flex overflow-hidden focus-within:border-red-500/50 transition-colors">
                <span class="px-4 flex items-center text-gray-400 text-sm font-mono border-l border-white/10">{{ selectedCountry.code }}</span>
                <input v-model="phone" type="tel" inputmode="numeric" placeholder="5XXXXXXXX"
                    class="flex-1 bg-transparent text-white px-4 py-4 outline-none placeholder-gray-700 text-base"
                    @keyup.enter="startLogin" />
            </div>
            <p v-if="error" class="bg-red-900/30 border border-red-800/50 rounded-xl px-4 py-2.5 text-red-400 text-xs">{{ error }}</p>
        </div>
        <div class="pb-12 pt-6">
            <button @click="startLogin" :disabled="!phone.trim() || loading"
                class="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-40"
                :class="phone.trim() ? 'bg-red-600 text-white shadow-lg shadow-red-900/30' : 'bg-white/10 text-gray-600'">
                {{ loading ? 'جاري الاتصال...' : 'التالي ←' }}
            </button>
        </div>
    </div>

    <!-- Code -->
    <div v-else-if="step === 'code'" class="relative z-10 flex flex-col flex-1 px-6">
        <div class="pt-12 pb-8">
            <h2 class="text-3xl font-black">رمز تيليجرام</h2>
            <p class="text-gray-500 mt-1 text-sm">أدخل الرمز المكوّن من 5 أرقام الذي وصلك على تيليجرام</p>
        </div>
        <div class="flex gap-3 justify-center mb-6" dir="ltr">
            <input v-for="(_, i) in 5" :key="i" :ref="el => codeInputs[i] = el"
                v-model="code[i]" type="tel" inputmode="numeric" maxlength="1"
                @input="onCodeInput(i, $event)" @keydown="onCodeKeydown(i, $event)"
                class="w-14 h-16 text-center text-2xl font-bold rounded-xl border-2 bg-white/8 outline-none transition-colors"
                :class="code[i] ? 'border-red-500 text-white' : 'border-white/15 text-white'" />
        </div>
        <p v-if="error" class="bg-red-900/30 border border-red-800/50 rounded-xl px-4 py-2.5 text-red-400 text-xs text-center mb-4">{{ error }}</p>
        <button @click="submitCode" :disabled="code.join('').length !== 5 || loading"
            class="w-full py-4 rounded-2xl font-bold text-base active:scale-95 transition-all disabled:opacity-40 bg-red-600 text-white">
            {{ loading ? 'جاري التحقق...' : 'تأكيد ←' }}
        </button>
    </div>

    <!-- 2FA -->
    <div v-else-if="step === 'password'" class="relative z-10 flex flex-col flex-1 px-6">
        <div class="pt-12 pb-8">
            <h2 class="text-3xl font-black">التحقق بخطوتين</h2>
            <p class="text-gray-500 mt-1 text-sm">أدخل كلمة مرور التحقق بخطوتين الخاصة بتيليجرام</p>
        </div>
        <div class="space-y-4 flex-1">
            <input v-model="password" type="password" placeholder="كلمة المرور"
                class="w-full bg-white/8 border border-white/12 rounded-2xl px-4 py-4 text-white outline-none focus:border-red-500/50 placeholder-gray-700"
                @keyup.enter="submitPassword" />
            <p v-if="error" class="bg-red-900/30 border border-red-800/50 rounded-xl px-4 py-2.5 text-red-400 text-xs">{{ error }}</p>
        </div>
        <div class="pb-12 pt-6">
            <button @click="submitPassword" :disabled="!password.trim() || loading"
                class="w-full py-4 rounded-2xl font-bold text-base bg-red-600 text-white active:scale-95 transition-all disabled:opacity-40">
                {{ loading ? 'جاري الدخول...' : 'دخول ←' }}
            </button>
        </div>
    </div>

</div>
</template>
