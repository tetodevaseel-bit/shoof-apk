<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../api.js';
import { useRouter } from 'vue-router';

const router = useRouter();
const sections = ref([]);
const loading = ref(true);
const searchQ = ref('');
const searching = ref(false);
const searchResults = ref([]);
const tab = ref('home'); // home | search | list | profile

onMounted(loadHome);

async function loadHome() {
    loading.value = true;
    try {
        const [discover] = await Promise.all([api.discover()]);
        sections.value = discover.sections || [];
    } catch {} finally { loading.value = false; }
}

async function doSearch() {
    if (!searchQ.value.trim()) return;
    searching.value = true;
    try {
        const data = await api.searchWatch(searchQ.value);
        searchResults.value = data.results || [];
    } catch {} finally { searching.value = false; }
}

function openChannel(uuid) {
    router.push(`/channel/${uuid}`);
}
</script>

<template>
    <div class="min-h-screen bg-gray-900 text-white pb-20">
        <!-- Header -->
        <header class="sticky top-0 z-20 bg-gray-900/95 backdrop-blur px-4 py-3 flex items-center gap-3 border-b border-gray-800">
            <div class="text-2xl">🎬</div>
            <h1 class="text-lg font-bold flex-1">شوف TV</h1>
        </header>

        <!-- Search bar -->
        <div v-if="tab === 'home' || tab === 'search'" class="px-4 pt-3 pb-1">
            <div class="relative">
                <input v-model="searchQ" @keyup.enter="doSearch" @input="tab = 'search'"
                    placeholder="🔍 ابحث عن مسلسل أو أنيمي..."
                    class="w-full bg-gray-800 rounded-full px-4 py-2.5 text-sm outline-none focus:ring-1 focus:ring-purple-500" />
            </div>
        </div>

        <!-- Search results -->
        <div v-if="tab === 'search'" class="p-4 space-y-2">
            <div v-if="searching" class="text-center text-gray-400 py-8">⏳ جاري البحث...</div>
            <div v-else-if="!searchResults.length && searchQ" class="text-center text-gray-400 py-8">لا نتائج</div>
            <div v-for="r in searchResults" :key="r.uuid" @click="openChannel(r.uuid)"
                class="bg-gray-800 rounded-xl p-3 flex items-center gap-3 cursor-pointer active:opacity-70">
                <img v-if="r.cover_url" :src="r.cover_url" class="w-14 h-14 rounded-lg object-cover shrink-0" />
                <div class="w-14 h-14 bg-gray-700 rounded-lg shrink-0 flex items-center justify-center" v-else>🎬</div>
                <div class="flex-1 min-w-0">
                    <div class="font-semibold truncate">{{ r.name }}</div>
                    <div class="text-xs text-gray-400">{{ r.episode_count }} حلقة</div>
                </div>
            </div>
        </div>

        <!-- Home sections -->
        <div v-else-if="tab === 'home'" class="p-4 space-y-6">
            <div v-if="loading" class="flex items-center justify-center py-16">
                <div class="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin"></div>
            </div>
            <template v-else v-for="section in sections" :key="section.key">
                <div>
                    <h2 class="font-bold text-base mb-3">
                        {{ section.key === 'newest' ? '🆕 الأحدث' :
                           section.key === 'continue' ? '▶️ أكمل المشاهدة' :
                           section.key === 'top10' ? '🔥 الأكثر مشاهدة' : section.key }}
                    </h2>
                    <div class="flex gap-3 overflow-x-auto pb-2 -mx-4 px-4">
                        <div v-for="item in section.items" :key="item.uuid"
                            @click="openChannel(item.uuid)"
                            class="shrink-0 w-32 cursor-pointer active:opacity-70">
                            <div class="relative rounded-xl overflow-hidden bg-gray-800">
                                <img v-if="item.cover_url" :src="item.cover_url"
                                    class="w-32 h-44 object-cover" loading="lazy" />
                                <div v-else class="w-32 h-44 flex items-center justify-center text-3xl">🎬</div>
                            </div>
                            <p class="text-xs mt-1.5 truncate font-medium">{{ item.name }}</p>
                        </div>
                    </div>
                </div>
            </template>
        </div>

        <!-- Bottom Nav -->
        <nav class="fixed bottom-0 inset-x-0 bg-gray-900/95 backdrop-blur border-t border-gray-800 flex z-30">
            <button v-for="t in [{k:'home',i:'🏠',l:'الرئيسية'},{k:'search',i:'🔍',l:'بحث'},{k:'list',i:'📋',l:'قائمتي'},{k:'profile',i:'👤',l:'حسابي'}]"
                :key="t.k" @click="tab = t.k"
                :class="['flex-1 py-2 flex flex-col items-center gap-0.5 text-[10px]',
                    tab === t.k ? 'text-purple-400' : 'text-gray-500']">
                <span class="text-xl">{{ t.i }}</span>
                {{ t.l }}
            </button>
        </nav>
    </div>
</template>
