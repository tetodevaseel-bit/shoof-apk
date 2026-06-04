<script setup>
import { ref } from 'vue';
import { TelegramClient } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';

const emit = defineEmits(['logged-in']);

const API_ID = 11704522;
const API_HASH = '4d8519ffb0302f8ac42574a44839bde3';

const step = ref('welcome');
const phone = ref('');
const fullPhone = ref('');
const code = ref(['', '', '', '', '']);
const password = ref('');
const showPassword = ref(false);
const error = ref('');
const loading = ref(false);
const selectedCountry = ref({ code: '+966', name: 'السعودية', flag: '🇸🇦' });
const showCountry = ref(false);
const codeInputs = ref([]);

let client = null;
let resolveCode = null;
let resolvePassword = null;

const countries = [
    { code: '+966', name: 'السعودية',    flag: '🇸🇦' },
    { code: '+971', name: 'الإمارات',    flag: '🇦🇪' },
    { code: '+965', name: 'الكويت',      flag: '🇰🇼' },
    { code: '+974', name: 'قطر',         flag: '🇶🇦' },
    { code: '+973', name: 'البحرين',     flag: '🇧🇭' },
    { code: '+968', name: 'عُمان',       flag: '🇴🇲' },
    { code: '+962', name: 'الأردن',      flag: '🇯🇴' },
    { code: '+961', name: 'لبنان',       flag: '🇱🇧' },
    { code: '+964', name: 'العراق',      flag: '🇮🇶' },
    { code: '+963', name: 'سوريا',       flag: '🇸🇾' },
    { code: '+967', name: 'اليمن',       flag: '🇾🇪' },
    { code: '+20',  name: 'مصر',         flag: '🇪🇬' },
    { code: '+218', name: 'ليبيا',       flag: '🇱🇾' },
    { code: '+213', name: 'الجزائر',     flag: '🇩🇿' },
    { code: '+216', name: 'تونس',        flag: '🇹🇳' },
    { code: '+212', name: 'المغرب',      flag: '🇲🇦' },
    { code: '+249', name: 'السودان',     flag: '🇸🇩' },
    { code: '+1',   name: 'أمريكا',      flag: '🇺🇸' },
    { code: '+44',  name: 'بريطانيا',    flag: '🇬🇧' },
    { code: '+90',  name: 'تركيا',       flag: '🇹🇷' },
    { code: '+7',   name: 'روسيا',       flag: '🇷🇺' },
    { code: '+49',  name: 'ألمانيا',     flag: '🇩🇪' },
    { code: '+33',  name: 'فرنسا',       flag: '🇫🇷' },
    { code: '+91',  name: 'الهند',       flag: '🇮🇳' },
    { code: '+92',  name: 'باكستان',     flag: '🇵🇰' },
    { code: '+62',  name: 'إندونيسيا',  flag: '🇮🇩' },
    { code: '+86',  name: 'الصين',       flag: '🇨🇳' },
    { code: '+81',  name: 'اليابان',     flag: '🇯🇵' },
    { code: '+55',  name: 'البرازيل',    flag: '🇧🇷' },
    { code: '+61',  name: 'أستراليا',    flag: '🇦🇺' },
    { code: '+98',  name: 'إيران',       flag: '🇮🇷' },
];

async function startLogin() {
    if (!phone.value.trim() || loading.value) return;
    loading.value = true;
    error.value = '';
    fullPhone.value = selectedCountry.value.code + phone.value.trim();
    step.value = 'connecting';

    try {
        client = new TelegramClient(new StringSession(''), API_ID, API_HASH, {
            connectionRetries: 5,
        });
        await client.connect();

        client.start({
            phoneNumber: async () => fullPhone.value,
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
            emit('logged-in', { client, session, me });
        }).catch(err => {
            error.value = err.message || 'خطأ في تسجيل الدخول';
            loading.value = false;
            step.value = 'phone';
        });

    } catch (err) {
        error.value = err.message || 'تعذّر الاتصال بتيليجرام';
        loading.value = false;
        step.value = 'phone';
    }
}

function onCodeInput(i, e) {
    const val = e.target.value.replace(/\D/g, '').slice(-1);
    code.value[i] = val;
    if (val && i < 4) codeInputs.value[i + 1]?.focus();
    if (code.value.filter(v => v).length === 5) submitCode();
}

function onCodeKeydown(i, e) {
    if (e.key === 'Backspace' && !code.value[i] && i > 0) {
        code.value[i - 1] = '';
        codeInputs.value[i - 1]?.focus();
    }
}

function onCodePaste(e) {
    const text = e.clipboardData?.getData('text')?.replace(/\D/g, '').slice(0, 5);
    if (!text) return;
    e.preventDefault();
    text.split('').forEach((ch, i) => { code.value[i] = ch; });
    codeInputs.value[Math.min(text.length, 4)]?.focus();
    if (text.length === 5) submitCode();
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

function resendCode() {
    code.value = ['', '', '', '', ''];
    error.value = '';
    step.value = 'phone';
}

function maskPhone(p) {
    if (!p) return '';
    const digits = p.replace(/\D/g, '');
    if (digits.length <= 4) return p;
    return p.slice(0, p.length - 4).replace(/\d/g, '•') + digits.slice(-4);
}
</script>

<template>
<div dir="rtl" class="fixed inset-0 bg-[#080808] text-white overflow-hidden flex flex-col select-none" style="font-family:'Cairo',sans-serif">

    <!-- ═══════════════════════════════════════════ -->
    <!-- شاشة الترحيب                               -->
    <!-- ═══════════════════════════════════════════ -->
    <transition name="fade">
    <div v-if="step === 'welcome'" class="flex flex-col flex-1">

        <!-- خلفية سينمائية -->
        <div class="absolute inset-0">
            <div class="absolute inset-0 bg-gradient-to-b from-[#1a0005] via-[#080808] to-[#080808]"></div>
            <div class="absolute top-0 left-0 right-0 h-96 opacity-30"
                style="background: radial-gradient(ellipse 100% 60% at 50% 0%, #c0001a 0%, transparent 70%)"></div>
        </div>

        <!-- محتوى الترحيب -->
        <div class="relative z-10 flex flex-col flex-1 items-center justify-between px-6 pt-24 pb-14">

            <!-- الشعار -->
            <div class="flex flex-col items-center gap-5">
                <!-- أيقونة -->
                <div class="relative">
                    <div class="w-28 h-28 rounded-[2rem] flex items-center justify-center shadow-2xl"
                        style="background: linear-gradient(135deg, #c0001a 0%, #7a0011 100%); box-shadow: 0 24px 64px rgba(192,0,26,0.45)">
                        <svg width="56" height="56" viewBox="0 0 56 56" fill="none">
                            <path d="M14 14 L14 42 L44 28 Z" fill="white" opacity="0.95"/>
                        </svg>
                    </div>
                    <div class="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-green-500 border-2 border-[#080808] flex items-center justify-center">
                        <div class="w-2 h-2 rounded-full bg-white"></div>
                    </div>
                </div>

                <!-- اسم التطبيق -->
                <div class="text-center">
                    <h1 class="text-5xl font-black tracking-tight leading-none">
                        شوف <span style="color:#e5001a">TV</span>
                    </h1>
                    <p class="text-[#666] mt-3 text-sm leading-relaxed tracking-wide">
                        آلاف المسلسلات والأفلام والأنيميات<br/>بجودة عالية في أي وقت ومكان
                    </p>
                </div>

                <!-- شارات الميزات -->
                <div class="flex gap-3 mt-2">
                    <span class="px-3 py-1 rounded-full text-xs font-semibold" style="background:#1a1a1a;color:#888">4K جودة</span>
                    <span class="px-3 py-1 rounded-full text-xs font-semibold" style="background:#1a1a1a;color:#888">بدون إعلانات</span>
                    <span class="px-3 py-1 rounded-full text-xs font-semibold" style="background:#1a1a1a;color:#888">مباشرة</span>
                </div>
            </div>

            <!-- أزرار الدخول -->
            <div class="w-full space-y-3">
                <button @click="step='phone'"
                    class="w-full py-4 rounded-2xl font-bold text-lg transition-all active:scale-95"
                    style="background:linear-gradient(135deg,#e5001a,#a80013);box-shadow:0 8px 32px rgba(229,0,26,0.35)">
                    ابدأ المشاهدة مجاناً
                </button>
                <p class="text-center text-xs" style="color:#444">
                    يتطلب حساب تيليجرام للدخول
                </p>
            </div>
        </div>

    </div>
    </transition>

    <!-- ═══════════════════════════════════════════ -->
    <!-- شاشة إدخال الرقم                           -->
    <!-- ═══════════════════════════════════════════ -->
    <transition name="slide">
    <div v-if="step === 'phone'" class="flex flex-col flex-1">

        <!-- رأس الصفحة -->
        <div class="flex items-center justify-between px-5 pt-14 pb-6">
            <button @click="step='welcome'; error=''" class="w-10 h-10 rounded-full flex items-center justify-center" style="background:#1a1a1a">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M9 18l6-6-6-6"/>
                </svg>
            </button>
            <span class="text-base font-bold" style="color:#e5001a">شوف TV</span>
            <div class="w-10"></div>
        </div>

        <div class="flex flex-col flex-1 px-6">

            <!-- العنوان -->
            <div class="mb-8">
                <h2 class="text-3xl font-black leading-tight">الدخول</h2>
                <p class="mt-2 text-sm leading-relaxed" style="color:#777">
                    أدخل رقم هاتفك المسجّل في تيليجرام
                </p>
            </div>

            <!-- اختيار الدولة -->
            <div class="space-y-3">
                <button @click="showCountry = !showCountry"
                    class="w-full rounded-2xl px-4 py-4 flex items-center gap-3 transition-all"
                    style="background:#151515; border:1px solid #2a2a2a">
                    <span class="text-2xl leading-none">{{ selectedCountry.flag }}</span>
                    <div class="flex-1 text-right">
                        <div class="text-sm font-semibold text-white">{{ selectedCountry.name }}</div>
                        <div class="text-xs font-mono" style="color:#555">{{ selectedCountry.code }}</div>
                    </div>
                    <svg :class="showCountry ? 'rotate-180' : ''" class="transition-transform" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#555" stroke-width="2.5">
                        <path d="M6 9l6 6 6-6"/>
                    </svg>
                </button>

                <!-- قائمة الدول -->
                <div v-if="showCountry"
                    class="rounded-2xl overflow-hidden overflow-y-auto"
                    style="background:#111; border:1px solid #2a2a2a; max-height:220px">
                    <button v-for="c in countries" :key="c.code"
                        @click="selectedCountry = c; showCountry = false"
                        class="w-full px-4 py-3.5 flex items-center gap-3 transition-colors active:opacity-60"
                        style="border-bottom:1px solid #1e1e1e">
                        <span class="text-xl leading-none">{{ c.flag }}</span>
                        <span class="flex-1 text-sm text-right text-white">{{ c.name }}</span>
                        <span class="text-xs font-mono" style="color:#555">{{ c.code }}</span>
                    </button>
                </div>

                <!-- حقل الرقم -->
                <div class="rounded-2xl overflow-hidden flex transition-all"
                    style="background:#151515; border:1px solid #2a2a2a"
                    :style="phone ? 'border-color:#e5001a40' : ''">
                    <span class="px-4 flex items-center text-sm font-mono font-semibold" style="color:#666; border-left:1px solid #2a2a2a; min-width:70px; justify-content:center">
                        {{ selectedCountry.code }}
                    </span>
                    <input
                        v-model="phone"
                        type="tel"
                        inputmode="numeric"
                        placeholder="5XXXXXXXX"
                        class="flex-1 bg-transparent text-white px-4 py-4 outline-none text-base font-mono"
                        style="placeholder-color:#333"
                        @keyup.enter="startLogin"
                    />
                </div>

                <!-- رسالة الخطأ -->
                <div v-if="error" class="rounded-xl px-4 py-3 text-sm" style="background:#2a0008; border:1px solid #5a0015; color:#ff6b7a">
                    {{ error }}
                </div>
            </div>

            <div class="flex-1"></div>

            <!-- زر التالي -->
            <div class="pb-10">
                <button @click="startLogin" :disabled="!phone.trim() || loading"
                    class="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-30"
                    :style="phone.trim() ? 'background:linear-gradient(135deg,#e5001a,#a80013);box-shadow:0 8px 24px rgba(229,0,26,0.3)' : 'background:#1e1e1e;color:#444'">
                    {{ loading ? 'جاري الاتصال...' : 'التالي' }}
                </button>
            </div>
        </div>
    </div>
    </transition>

    <!-- ═══════════════════════════════════════════ -->
    <!-- شاشة الاتصال                               -->
    <!-- ═══════════════════════════════════════════ -->
    <div v-if="step === 'connecting'" class="flex flex-col flex-1 items-center justify-center gap-6 px-8">
        <div class="relative">
            <div class="w-20 h-20 rounded-[1.5rem] flex items-center justify-center"
                style="background:linear-gradient(135deg,#e5001a,#7a0011)">
                <svg width="36" height="36" viewBox="0 0 56 56" fill="none">
                    <path d="M14 14 L14 42 L44 28 Z" fill="white"/>
                </svg>
            </div>
            <!-- دوّار -->
            <div class="absolute -inset-2 rounded-[2rem] border-2 border-transparent animate-spin"
                style="border-top-color:#e5001a; border-right-color:#e5001a40"></div>
        </div>
        <div class="text-center">
            <p class="font-bold text-base text-white">جاري الاتصال بتيليجرام</p>
            <p class="text-sm mt-1" style="color:#555">يرجى الانتظار...</p>
        </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- شاشة إدخال كود التحقق                      -->
    <!-- ═══════════════════════════════════════════ -->
    <transition name="slide">
    <div v-if="step === 'code'" class="flex flex-col flex-1">

        <!-- رأس -->
        <div class="flex items-center justify-between px-5 pt-14 pb-6">
            <button @click="resendCode" class="w-10 h-10 rounded-full flex items-center justify-center" style="background:#1a1a1a">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M9 18l6-6-6-6"/>
                </svg>
            </button>
            <span class="text-base font-bold" style="color:#e5001a">شوف TV</span>
            <div class="w-10"></div>
        </div>

        <div class="flex flex-col flex-1 px-6">

            <!-- أيقونة تيليجرام -->
            <div class="flex justify-center mb-6">
                <div class="w-16 h-16 rounded-2xl flex items-center justify-center"
                    style="background:linear-gradient(135deg,#2AABEE,#1a8bc8);box-shadow:0 8px 24px rgba(42,171,238,0.3)">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="white">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8l-1.68 7.92c-.12.56-.48.7-.96.44l-2.64-1.96-1.28 1.24c-.14.14-.26.26-.52.26l.18-2.6 4.72-4.28c.2-.18-.04-.28-.32-.1l-5.84 3.68-2.52-.8c-.54-.18-.56-.54.12-.8l9.84-3.8c.46-.16.86.12.9.8z"/>
                    </svg>
                </div>
            </div>

            <!-- العنوان -->
            <div class="text-center mb-8">
                <h2 class="text-2xl font-black">رمز التحقق</h2>
                <p class="mt-2 text-sm" style="color:#666">
                    أرسلنا رمزاً مكوناً من 5 أرقام إلى تيليجرام
                </p>
                <p class="mt-1 text-sm font-bold" style="color:#e5001a" dir="ltr">
                    {{ fullPhone }}
                </p>
            </div>

            <!-- صناديق الكود -->
            <div class="flex justify-center gap-3 mb-8 rtl:flex-row-reverse" dir="ltr">
                <input
                    v-for="(_, i) in 5" :key="i"
                    :ref="el => codeInputs[i] = el"
                    v-model="code[i]"
                    type="tel"
                    inputmode="numeric"
                    maxlength="1"
                    @input="onCodeInput(i, $event)"
                    @keydown="onCodeKeydown(i, $event)"
                    @paste="onCodePaste"
                    class="w-14 h-16 text-center text-2xl font-black rounded-2xl outline-none transition-all"
                    :style="code[i]
                        ? 'background:#1e0004; border:2px solid #e5001a; color:#fff; box-shadow:0 0 0 3px rgba(229,0,26,0.15)'
                        : 'background:#151515; border:2px solid #2a2a2a; color:#fff'"
                />
            </div>

            <!-- خطأ -->
            <div v-if="error" class="rounded-xl px-4 py-3 text-sm text-center mb-4" style="background:#2a0008; border:1px solid #5a0015; color:#ff6b7a">
                {{ error }}
            </div>

            <div class="flex-1"></div>

            <!-- أزرار -->
            <div class="pb-10 space-y-3">
                <button @click="submitCode" :disabled="code.join('').length !== 5 || loading"
                    class="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-30"
                    style="background:linear-gradient(135deg,#e5001a,#a80013);box-shadow:0 8px 24px rgba(229,0,26,0.3)">
                    {{ loading ? 'جاري التحقق...' : 'تأكيد' }}
                </button>
                <button @click="resendCode" class="w-full py-3 rounded-2xl text-sm font-semibold transition-all active:opacity-60" style="color:#666">
                    لم يصلني الرمز؟ إعادة المحاولة
                </button>
            </div>
        </div>
    </div>
    </transition>

    <!-- ═══════════════════════════════════════════ -->
    <!-- شاشة التحقق بخطوتين                        -->
    <!-- ═══════════════════════════════════════════ -->
    <div v-if="step === 'password'" class="flex flex-col flex-1 px-6">

        <div class="flex items-center justify-between px-0 pt-14 pb-6">
            <button @click="step='code'; error=''" class="w-10 h-10 rounded-full flex items-center justify-center" style="background:#1a1a1a">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M9 18l6-6-6-6"/>
                </svg>
            </button>
        </div>

        <!-- أيقونة القفل -->
        <div class="flex justify-center mb-6">
            <div class="w-16 h-16 rounded-2xl flex items-center justify-center" style="background:#1a1a1a">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#e5001a" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
            </div>
        </div>

        <div class="text-center mb-8">
            <h2 class="text-2xl font-black">التحقق بخطوتين</h2>
            <p class="mt-2 text-sm" style="color:#666">
                أدخل كلمة مرور التحقق بخطوتين الخاصة بحسابك في تيليجرام
            </p>
        </div>

        <div class="relative mb-3">
            <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="كلمة المرور"
                class="w-full rounded-2xl px-4 py-4 text-white outline-none text-base"
                style="background:#151515; border:1px solid #2a2a2a; padding-left:3rem"
                @keyup.enter="submitPassword"
            />
            <button @click="showPassword = !showPassword"
                class="absolute left-4 top-1/2 -translate-y-1/2"
                style="color:#555">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path v-if="!showPassword" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle v-if="!showPassword" cx="12" cy="12" r="3"/>
                    <path v-if="showPassword" d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line v-if="showPassword" x1="1" y1="1" x2="23" y2="23"/>
                </svg>
            </button>
        </div>

        <div v-if="error" class="rounded-xl px-4 py-3 text-sm mb-4" style="background:#2a0008; border:1px solid #5a0015; color:#ff6b7a">
            {{ error }}
        </div>

        <div class="flex-1"></div>

        <div class="pb-10">
            <button @click="submitPassword" :disabled="!password.trim() || loading"
                class="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-30"
                style="background:linear-gradient(135deg,#e5001a,#a80013);box-shadow:0 8px 24px rgba(229,0,26,0.3)">
                {{ loading ? 'جاري الدخول...' : 'دخول' }}
            </button>
        </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- شاشة التحميل النهائية                      -->
    <!-- ═══════════════════════════════════════════ -->
    <div v-if="step === 'loading'" class="flex flex-col flex-1 items-center justify-center gap-8 px-8">
        <!-- دوائر متحركة -->
        <div class="relative flex items-center justify-center">
            <div class="absolute w-32 h-32 rounded-full border-2 opacity-20 animate-ping" style="border-color:#e5001a"></div>
            <div class="absolute w-24 h-24 rounded-full border-2 opacity-30 animate-ping" style="border-color:#e5001a; animation-delay:0.3s"></div>
            <div class="w-20 h-20 rounded-[1.5rem] flex items-center justify-center"
                style="background:linear-gradient(135deg,#e5001a,#7a0011);box-shadow:0 16px 48px rgba(229,0,26,0.4)">
                <svg width="36" height="36" viewBox="0 0 56 56" fill="none">
                    <path d="M14 14 L14 42 L44 28 Z" fill="white"/>
                </svg>
            </div>
        </div>
        <div class="text-center">
            <p class="font-bold text-lg text-white">مرحباً بك في شوف TV</p>
            <p class="text-sm mt-2" style="color:#555">جاري تحضير المحتوى...</p>
        </div>
    </div>

</div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from { opacity: 0; transform: translateX(-20px); }
.slide-leave-to { opacity: 0; transform: translateX(20px); }

@keyframes spin-slow { to { transform: rotate(360deg); } }
.animate-spin { animation: spin-slow 1s linear infinite; }
</style>
