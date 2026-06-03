import { createRouter, createWebHashHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import ChannelView from '../views/ChannelView.vue';

export default createRouter({
    history: createWebHashHistory(),
    routes: [
        { path: '/', component: HomeView },
        { path: '/channel/:uuid', component: ChannelView, props: true },
    ],
});
