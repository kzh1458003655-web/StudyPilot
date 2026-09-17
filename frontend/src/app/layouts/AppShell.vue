<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArchiveIcon,
  BookOpenIcon,
  MenuIcon,
  MoreHorizontalIcon,
  PlusIcon,
  RotateCcwIcon,
} from "@lucide/vue";
import { toast } from "vue-sonner";
import {
  archiveProject,
  createProject,
  listProjects,
  restoreProject,
} from "@/modules/project/api/requests";
import type { StudyProject } from "@/modules/project/types/domain";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import { Input } from "@/shared/ui/input";
import { Textarea } from "@/shared/ui/textarea";
import { ScrollArea } from "@/shared/ui/scroll-area";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/shared/ui/tooltip";

type CourseModule = "qa" | "exams" | "frequency";

const route = useRoute();
const router = useRouter();
const projects = ref<StudyProject[]>([]);
const loading = ref(false);
const loadFailed = ref(false);
const courseDialogOpen = ref(false);
const archiveSheetOpen = ref(false);
const mobileSheetOpen = ref(false);
const pendingArchive = ref<StudyProject>();
const courseName = ref("");
const courseDescription = ref("");
const creatingCourse = ref(false);
const courseError = ref("");
const archiveRedirecting = ref(false);

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
const currentSection = computed<CourseModule>(() => {
  if (route.path.endsWith("/frequency")) return "frequency";
  if (route.path.includes("/exams") || route.path.includes("/assessment")) {
    return "exams";
  }
  return "qa";
});

function projectRoute(id: number, section: CourseModule = "qa") {
  return `/projects/${id}/${section}`;
}

function formatArchivedAt(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

async function refreshProjects() {
  loading.value = true;
  loadFailed.value = false;
  try {
    projects.value = await listProjects(true);
    if (route.query.archive === "1") {
      archiveSheetOpen.value = true;
    }
  } catch {
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
}

async function openProject(project: StudyProject) {
  mobileSheetOpen.value = false;
  await router.push(projectRoute(project.id));
}

function openCourseDialog() {
  courseError.value = "";
  courseDialogOpen.value = true;
}

async function archiveCourse(project: StudyProject) {
  try {
    await archiveProject(project.id);
    pendingArchive.value = undefined;
    await refreshProjects();
    if (project.id === projectId.value) await router.push("/projects");
    toast.success("课程已归档");
  } catch {
    toast.error("课程归档失败，请稍后重试");
  }
}

async function restoreCourse(project: StudyProject) {
  try {
    await restoreProject(project.id);
    await refreshProjects();
    toast.success("课程已恢复，可从课程列表打开");
  } catch {
    toast.error("课程恢复失败，请稍后重试");
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
    await router.push(projectRoute(project.id));
    toast.success("课程已创建");
  } catch {
    courseError.value = "课程创建失败，请确认本地服务已启动。";
  } finally {
    creatingCourse.value = false;
  }
}

function changeSection(value: string | number) {
  if (!projectId.value) return;
  router.push(projectRoute(projectId.value, String(value) as CourseModule));
}

onMounted(refreshProjects);
watch(
  () => route.query.create,
  (value) => {
    if (value === "1") openCourseDialog();
  },
  { immediate: true },
);
watch(
  activeProject,
  async (project) => {
    if (!project?.archivedAt || archiveRedirecting.value) return;
    archiveRedirecting.value = true;
    archiveSheetOpen.value = true;
    toast.info("请先恢复课程，再查看课程内容");
    await router.replace({ path: "/projects", query: { archive: "1" } });
    archiveRedirecting.value = false;
  },
  { immediate: true },
);
</script>

<template>
  <TooltipProvider>
    <div
      class="min-h-screen bg-background text-foreground md:grid md:grid-cols-[276px_minmax(0,1fr)]"
    >
      <aside class="hidden h-screen border-r bg-[#f5f1ea] md:flex md:flex-col">
        <RouterLink
          class="flex h-20 items-center gap-3 px-6"
          to="/projects"
          aria-label="StudyPilot 课程主页"
        >
          <span
            class="grid size-10 place-items-center rounded-xl bg-foreground font-serif text-xl font-semibold text-background"
            >S</span
          >
          <span class="min-w-0">
            <strong class="block text-lg tracking-tight">StudyPilot</strong>
            <small
              class="block text-[10px] tracking-[0.24em] text-muted-foreground"
              >LOCAL STUDY SPACE</small
            >
          </span>
        </RouterLink>

        <div class="px-4 pb-4">
          <Button
            class="h-11 w-full justify-start bg-card text-foreground shadow-none ring-1 ring-border hover:bg-accent"
            @click="openCourseDialog"
          >
            <PlusIcon />新建课程
          </Button>
        </div>

        <div class="flex min-h-0 flex-1 flex-col px-3 pb-3">
          <div
            class="flex items-center justify-between px-3 py-2 text-xs text-muted-foreground"
          >
            <span>课程</span><span>{{ visibleProjects.length }}</span>
          </div>
          <ScrollArea class="min-h-0 flex-1 pr-2">
            <div class="space-y-1 pb-6" aria-label="活动课程列表">
              <p
                v-if="loading"
                class="px-3 py-8 text-center text-sm text-muted-foreground"
              >
                正在读取课程…
              </p>
              <div v-else-if="loadFailed" class="px-3 py-8 text-center">
                <p class="text-sm text-muted-foreground">本地服务未连接</p>
                <Button
                  variant="ghost"
                  size="sm"
                  class="mt-2"
                  @click="refreshProjects"
                  >重试</Button
                >
              </div>
              <p
                v-else-if="!visibleProjects.length"
                class="px-3 py-8 text-center text-sm text-muted-foreground"
              >
                还没有课程
              </p>
              <div
                v-for="project in visibleProjects"
                :key="project.id"
                class="group relative flex items-center"
              >
                <Tooltip>
                  <TooltipTrigger as-child>
                    <button
                      class="relative flex min-h-12 min-w-0 flex-1 items-center gap-2 rounded-lg px-3 py-2 pr-9 text-left text-sm transition-colors hover:bg-accent/70"
                      :class="
                        project.id === projectId
                          ? 'bg-accent font-medium before:absolute before:inset-y-2 before:left-0 before:w-0.5 before:rounded-full before:bg-primary'
                          : 'text-muted-foreground'
                      "
                      type="button"
                      @click="openProject(project)"
                    >
                      <BookOpenIcon class="size-4 shrink-0" />
                      <span class="line-clamp-2 leading-5">{{
                        project.name
                      }}</span>
                    </button>
                  </TooltipTrigger>
                  <TooltipContent side="right" class="max-w-72">{{
                    project.name
                  }}</TooltipContent>
                </Tooltip>
                <DropdownMenu>
                  <DropdownMenuTrigger as-child>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      class="absolute right-1 opacity-0 group-hover:opacity-100 focus:opacity-100"
                      :aria-label="`课程操作：${project.name}`"
                    >
                      <MoreHorizontalIcon />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start">
                    <DropdownMenuItem
                      class="text-destructive"
                      @select="pendingArchive = project"
                    >
                      <ArchiveIcon />归档课程
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>
          </ScrollArea>
        </div>
      </aside>

      <main class="min-w-0">
        <header
          class="sticky top-0 z-30 flex h-16 items-center gap-3 border-b bg-background/92 px-4 backdrop-blur md:grid md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:px-7"
        >
          <Button
            variant="ghost"
            size="icon-sm"
            class="md:hidden"
            aria-label="打开课程列表"
            @click="mobileSheetOpen = true"
          >
            <MenuIcon />
          </Button>
          <p
            class="hidden min-w-0 items-center gap-2 text-sm text-muted-foreground md:flex"
          >
            <RouterLink
              to="/projects"
              class="shrink-0 whitespace-nowrap hover:text-foreground"
              >我的课程</RouterLink
            >
            <span class="shrink-0">/</span>
            <span class="truncate text-foreground">{{
              activeProject?.name ?? "课程工作台"
            }}</span>
          </p>
          <Tabs
            v-if="projectId"
            :model-value="currentSection"
            @update:model-value="changeSection"
          >
            <TabsList class="h-10 bg-secondary/80 p-1">
              <TabsTrigger value="qa" class="px-4">问答</TabsTrigger>
              <TabsTrigger value="exams" class="px-4">智能组卷</TabsTrigger>
              <TabsTrigger value="frequency" class="px-4">考频分析</TabsTrigger>
            </TabsList>
          </Tabs>
          <span v-else class="font-serif text-base font-semibold md:hidden"
            >StudyPilot</span
          >
          <div class="ml-auto flex justify-end">
            <Button
              variant="ghost"
              size="sm"
              class="gap-2"
              @click="archiveSheetOpen = true"
            >
              <ArchiveIcon /><span class="hidden sm:inline">归档课程</span>
              <Badge
                v-if="archivedProjects.length"
                variant="secondary"
                class="min-w-5 justify-center px-1.5"
                >{{ archivedProjects.length }}</Badge
              >
            </Button>
          </div>
        </header>
        <div class="min-h-[calc(100vh-4rem)]"><slot /></div>
      </main>

      <Sheet v-model:open="archiveSheetOpen">
        <SheetContent class="w-[92vw] border-l bg-card p-0 sm:max-w-md">
          <SheetHeader class="border-b px-6 py-5 text-left">
            <SheetTitle>归档课程</SheetTitle>
            <SheetDescription class="sr-only"
              >恢复后可从活动课程列表重新进入课程。</SheetDescription
            >
          </SheetHeader>
          <ScrollArea class="h-[calc(100vh-76px)]">
            <div class="space-y-3 p-5">
              <p
                v-if="!archivedProjects.length"
                class="py-16 text-center text-sm text-muted-foreground"
              >
                暂无归档课程
              </p>
              <article
                v-for="project in archivedProjects"
                :key="project.id"
                class="rounded-xl border bg-background p-4"
              >
                <h3
                  class="break-words font-sans text-sm font-medium tracking-normal"
                >
                  {{ project.name }}
                </h3>
                <p class="mt-1 text-xs text-muted-foreground">
                  归档于 {{ formatArchivedAt(project.archivedAt) }}
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  class="mt-4 w-full"
                  @click="restoreCourse(project)"
                >
                  <RotateCcwIcon />恢复课程
                </Button>
              </article>
            </div>
          </ScrollArea>
        </SheetContent>
      </Sheet>

      <Sheet v-model:open="mobileSheetOpen">
        <SheetContent side="left" class="w-[88vw] bg-[#f5f1ea] p-0 sm:max-w-sm">
          <SheetHeader class="border-b px-5 py-5 text-left"
            ><SheetTitle>选择课程</SheetTitle
            ><SheetDescription class="sr-only"
              >从活动课程中选择要打开的课程。</SheetDescription
            ></SheetHeader
          >
          <div class="p-4">
            <Button class="w-full" @click="openCourseDialog"
              ><PlusIcon />新建课程</Button
            >
          </div>
          <ScrollArea class="h-[calc(100vh-145px)] px-3">
            <button
              v-for="project in visibleProjects"
              :key="project.id"
              class="mb-1 flex w-full items-start gap-2 rounded-lg px-3 py-3 text-left text-sm hover:bg-accent"
              @click="openProject(project)"
            >
              <BookOpenIcon class="mt-0.5 size-4 shrink-0" /><span
                class="break-words"
                >{{ project.name }}</span
              >
            </button>
          </ScrollArea>
        </SheetContent>
      </Sheet>

      <Dialog v-model:open="courseDialogOpen">
        <DialogContent class="sm:max-w-md">
          <DialogHeader
            ><DialogTitle>新建课程</DialogTitle
            ><DialogDescription class="sr-only"
              >填写课程名称和可选说明。</DialogDescription
            ></DialogHeader
          >
          <form class="space-y-4" @submit.prevent="createCourse">
            <label class="grid gap-2 text-sm font-medium"
              >课程名称
              <Input
                v-model="courseName"
                autofocus
                maxlength="100"
                placeholder="例如：操作系统期末复习"
              />
            </label>
            <label class="grid gap-2 text-sm font-medium"
              >说明 <span class="sr-only">可选</span>
              <Textarea
                v-model="courseDescription"
                maxlength="1000"
                placeholder="可选"
                class="min-h-24 resize-none"
              />
            </label>
            <p v-if="courseError" class="text-sm text-destructive" role="alert">
              {{ courseError }}
            </p>
            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                @click="courseDialogOpen = false"
                >取消</Button
              >
              <Button :disabled="creatingCourse" type="submit">{{
                creatingCourse ? "正在创建…" : "创建课程"
              }}</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <AlertDialog
        :open="Boolean(pendingArchive)"
        @update:open="(open) => !open && (pendingArchive = undefined)"
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle
              >归档“{{ pendingArchive?.name }}”？</AlertDialogTitle
            >
            <AlertDialogDescription
              >归档后需先恢复课程，才能继续查看其中内容。</AlertDialogDescription
            >
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel @click="pendingArchive = undefined"
              >取消</AlertDialogCancel
            >
            <AlertDialogAction
              class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              @click="pendingArchive && archiveCourse(pendingArchive)"
              >归档</AlertDialogAction
            >
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  </TooltipProvider>
</template>
