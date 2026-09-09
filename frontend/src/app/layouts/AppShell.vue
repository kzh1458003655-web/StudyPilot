<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { listProjects } from "@/modules/project/api/requests";
import type { StudyProject } from "@/modules/project/types/domain";

const route = useRoute();
const router = useRouter();
const projects = ref<StudyProject[]>([]);
const loading = ref(false);
const loadFailed = ref(false);
const quotes = [
  "慢慢来，把今天的一个问题弄懂就很好。",
  "暂时不会，是理解开始的地方。",
  "认真走过的每一步，都会成为你的底气。",
  "把复杂的问题拆小，答案就会慢慢清晰。",
];
const quote = quotes[Math.floor(Math.random() * quotes.length)];

const projectId = computed(() => {
  const value = Number(route.params.projectId);
  return Number.isInteger(value) && value > 0 ? value : undefined;
});
const activeProject = computed(() =>
  projects.value.find((project) => project.id === projectId.value),
);
const currentSection = computed(() => {
  if (route.path.endsWith("/resources")) return "resources";
  if (route.path.endsWith("/exams")) return "exams";
  if (route.path.endsWith("/assessment")) return "assessment";
  return "qa";
});

async function refreshProjects() {
  loading.value = true;
  loadFailed.value = false;
  try {
    projects.value = await listProjects();
  } catch {
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
}
function openProject(project: StudyProject) {
  router.push(`/projects/${project.id}/qa`);
}
function createCourse() {
  router.push("/projects");
}

onMounted(refreshProjects);
// Creating a project changes the route, so the course list refreshes automatically.
watch(() => route.fullPath, refreshProjects);
</script>

<template>
  <div class="study-shell">
    <aside class="course-sidebar">
      <RouterLink class="brand" to="/projects" aria-label="StudyPilot 课程主页">
        <span class="brand-mark">S</span>
        <span><strong>StudyPilot</strong><small>LOCAL STUDY SPACE</small></span>
      </RouterLink>
      <button class="new-course" type="button" @click="createCourse">
        <span>＋</span> 新建课程
      </button>

      <section class="course-section" aria-label="课程列表">
        <div class="side-heading">
          <span>我的课程</span><small>{{ projects.length }}</small>
        </div>
        <div class="course-list">
          <p v-if="loading" class="side-muted">正在读取课程…</p>
          <p v-else-if="loadFailed" class="side-muted">本地服务未连接</p>
          <p v-else-if="!projects.length" class="side-muted">
            还没有课程，先新建一个。
          </p>
          <button
            v-for="project in projects"
            :key="project.id"
            class="course-item"
            :class="{ selected: project.id === projectId }"
            type="button"
            @click="openProject(project)"
          >
            <span class="course-folder">□</span><span>{{ project.name }}</span>
          </button>
        </div>
      </section>

      <nav v-if="projectId" class="module-nav" aria-label="当前课程功能">
        <p class="side-heading">当前课程</p>
        <RouterLink
          :class="{ active: currentSection === 'qa' }"
          :to="`/projects/${projectId}/qa`"
        >
          问答
        </RouterLink>
        <RouterLink
          :class="{ active: currentSection === 'resources' }"
          :to="`/projects/${projectId}/resources`"
        >
          资料库
        </RouterLink>
        <RouterLink
          :class="{ active: currentSection === 'exams' }"
          :to="`/projects/${projectId}/exams`"
        >
          模拟考
        </RouterLink>
        <RouterLink
          :class="{ active: currentSection === 'assessment' }"
          :to="`/projects/${projectId}/assessment`"
        >
          测评
        </RouterLink>
      </nav>

      <div class="study-quote"><span>学习提醒</span>{{ quote }}</div>
      <div class="local-state">
        <i /> 本地学习空间 <small>课程资料互不共享</small>
      </div>
    </aside>
    <main class="main-pane">
      <header class="workspace-header">
        <p>
          <span>我的课程</span><b>/</b>{{ activeProject?.name ?? "课程工作台" }}
        </p>
        <span class="local-pill">本地模式</span>
      </header>
      <div class="workspace-content"><slot /></div>
    </main>
  </div>
</template>
