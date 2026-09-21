import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import "./styles.css";
import { router } from "./router";
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount("#app");
