<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  archiveProject,
  createProject,
  listProjects,
  restoreProject,
} from "@/modules/project/api/requests";
import type { StudyProject } from "@/modules/project/types/domain";

const route = useRoute();
const router = useRouter();
const projects = ref<StudyProject[]>([]);
const loading = ref(false);
const loadFailed = ref(false);
const courseDialogOpen = ref(false);
const courseName = ref("");
const courseDescription = ref("");
const creatingCourse = ref(false);
const courseError = ref("");
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
const visibleProjects = computed(() =>
  projects.value.filter((project) => !project.archivedAt),
);
const archivedProjects = computed(() =>
  projects.value.filter((project) => project.archivedAt),
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
    projects.value = await listProjects(true);
  } catch {
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
}
function openProject(project: StudyProject) {
  router.push(`/projects/${project.id}/qa`);
}
function openCourseDialog() {
  courseError.value = "";
  courseDialogOpen.value = true;
}
async function archiveCurrentCourse() {
  if (!activeProject.value) return;
  if (
    !window.confirm(`归档“${activeProject.value.name}”吗？资料和记录会保留。`)
  )
    return;
  try {
    await archiveProject(activeProject.value.id);
    await refreshProjects();
    await router.push("/projects");
  } catch {
    courseError.value = "课程归档失败，请稍后重试。";
  }
}
async function restoreCourse(project: StudyProject) {
  try {
    await restoreProject(project.id);
    await refreshProjects();
    await router.push(`/projects/${project.id}/qa`);
  } catch {
    courseError.value = "课程恢复失败，请稍后重试。";
  }
}
async function createCourse() {
  if (!courseName.value.trim()) {
    courseError.value = "请填写课程名称。";
    return;
  }
  creatingCourse.value = true;
  courseError.value = "";
  try {
    const project = await createProject(
      courseName.value.trim(),
      courseDescription.value.trim(),
    );
    await refreshProjects();
    courseDialogOpen.value = false;
    courseName.value = "";
    courseDescription.value = "";
    await router.push(`/projects/${project.id}/qa`);
  } catch {
    courseError.value = "课程创建失败，请确认本地服务已启动。";
  } finally {
    creatingCourse.value = false;
  }
}

onMounted(refreshProjects);
// Creating a project changes the route, so the course list refreshes automatically.
watch(
  () => route.fullPath,
  () => {
    refreshProjects();
    if (route.query.create === "1") openCourseDialog();
  },
);
</script>

<template>
  <div class="study-shell">
    <aside class="course-sidebar">
      <RouterLink class="brand" to="/projects" aria-label="StudyPilot 课程主页">
        <span class="brand-mark">S</span>
        <span><strong>StudyPilot</strong><small>LOCAL STUDY SPACE</small></span>
      </RouterLink>
      <button class="new-course" type="button" @click="openCourseDialog">
        <span>＋</span> 新建课程
      </button>

      <section class="course-section" aria-label="课程列表">
        <div class="side-heading">
          <span>我的课程</span><small>{{ visibleProjects.length }}</small>
        </div>
        <div class="course-list">
          <p v-if="loading" class="side-muted">正在读取课程…</p>
          <p v-else-if="loadFailed" class="side-muted">本地服务未连接</p>
          <p v-else-if="!visibleProjects.length" class="side-muted">
            还没有课程，先新建一个。
          </p>
          <button
            v-for="project in visibleProjects"
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

      <section
        v-if="archivedProjects.length"
        class="course-section archived-section"
        aria-label="已归档课程"
      >
        <div class="side-heading">
          <span>已归档</span><small>{{ archivedProjects.length }}</small>
        </div>
        <div class="course-list">
          <div
            v-for="project in archivedProjects"
            :key="project.id"
            class="archived-course"
          >
            <span>□ {{ project.name }}</span>
            <button type="button" @click="restoreCourse(project)">恢复</button>
          </div>
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
        <button
          class="archive-course"
          type="button"
          @click="archiveCurrentCourse"
        >
          归档本课程
        </button>
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
    <div
      v-if="courseDialogOpen"
      class="modal-backdrop"
      @click.self="courseDialogOpen = false"
    >
      <section
        class="course-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="course-dialog-title"
      >
        <p class="eyebrow">NEW COURSE</p>
        <h2 id="course-dialog-title">新建课程</h2>
        <p class="subtle">每门课程都有独立的资料、问答、模拟考和测评记录。</p>
        <form class="course-dialog-form" @submit.prevent="createCourse">
          <label
            >课程名称<input
              v-model="courseName"
              autofocus
              maxlength="100"
              placeholder="例如：操作系统期末复习"
          /></label>
          <label
            >说明（可选）<textarea
              v-model="courseDescription"
              maxlength="1000"
              placeholder="写下这门课程的学习目标"
            />
          </label>
          <p v-if="courseError" class="error" role="alert">{{ courseError }}</p>
          <div class="dialog-actions">
            <button
              class="secondary-button"
              type="button"
              @click="courseDialogOpen = false"
            >
              取消
            </button>
            <button :disabled="creatingCourse" type="submit">
              {{ creatingCourse ? "正在创建…" : "创建课程" }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>
