<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  FileTextIcon,
  MoreHorizontalIcon,
  PaperclipIcon,
  Trash2Icon,
  UploadCloudIcon,
} from "@lucide/vue";
import { toast } from "vue-sonner";
import { toAppError } from "@/shared/api/http";
import {
  deleteDocument,
  listDocuments,
  uploadDocument,
} from "@/modules/document/api/requests";
import type { CourseDocument } from "@/modules/document/api/requests";
import {
  Conversation,
  ConversationContent,
  ConversationScrollButton,
} from "@/shared/conversation";
import { Message, MessageContent } from "@/shared/message";
import {
  PromptInput,
  PromptInputBody,
  PromptInputFooter,
  PromptInputSubmit,
  PromptInputTextarea,
  PromptInputTools,
} from "@/shared/prompt-input";
import type { PromptInputMessage } from "@/shared/prompt-input";
import {
  InlineCitation,
  InlineCitationCard,
  InlineCitationCardBody,
  InlineCitationCardTrigger,
  InlineCitationQuote,
  InlineCitationSource,
} from "@/shared/inline-citation";
import { Attachments, Attachment } from "@/shared/attachments";
import { Suggestion, Suggestions } from "@/shared/suggestion";
import { Shimmer } from "@/shared/shimmer";
import { Loader } from "@/shared/loader";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
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
import { ScrollArea } from "@/shared/ui/scroll-area";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import { getQaHistory } from "../api/requests";
import type { QaAnswer } from "../types/domain";
import { useQaTaskStore } from "../stores/qaTaskStore";

interface Turn {
  question: string;
  answer: QaAnswer;
}

const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const question = ref("");
const composerKey = ref(0);
const sessionId = ref<number>();
const turns = ref<Turn[]>([]);
const tasks = useQaTaskStore();
const asking = computed(() => Boolean(tasks.active[projectId.value]));
const errorMessage = ref("");
const documents = ref<CourseDocument[]>([]);
const documentsLoading = ref(false);
const documentError = ref("");
const selectedFile = ref<File>();
const uploading = ref(false);
const fileInput = ref<HTMLInputElement>();
const materialsOpen = ref(false);
const pendingDelete = ref<CourseDocument>();
const MAX_PDF_BYTES = 25 * 1024 * 1024;
let mounted = true;

function statusLabel(status: string) {
  return (
    (
      {
        READY: "已就绪",
        PROCESSING: "处理中",
        PENDING: "等待中",
        FAILED: "失败",
      } as Record<string, string>
    )[status] ?? status
  );
}

function statusVariant(status: string) {
  return status === "FAILED"
    ? "destructive"
    : status === "READY"
      ? "secondary"
      : "outline";
}

async function loadHistory() {
  if (!Number.isInteger(projectId.value) || projectId.value <= 0) return;
  const targetProject = projectId.value;
  const history = await getQaHistory(targetProject);
  if (!mounted || targetProject !== projectId.value) return;
  sessionId.value = history.sessionId ?? undefined;
  turns.value = history.turns;
}

async function restoreHistory() {
  errorMessage.value = "";
  const running = tasks.current(projectId.value);
  try {
    await loadHistory();
    if (running) {
      try {
        await running;
      } catch (error) {
        if (mounted) errorMessage.value = toAppError(error).message;
      }
      await loadHistory();
    }
  } catch (error) {
    if (mounted) errorMessage.value = toAppError(error).message;
  }
}

async function loadDocuments() {
  if (!Number.isInteger(projectId.value) || projectId.value <= 0) return;
  const targetProject = projectId.value;
  documentsLoading.value = true;
  documentError.value = "";
  try {
    const result = await listDocuments(targetProject);
    if (mounted && targetProject === projectId.value) documents.value = result;
  } catch (error) {
    if (mounted && targetProject === projectId.value)
      documentError.value = toAppError(error).message;
  } finally {
    if (mounted && targetProject === projectId.value)
      documentsLoading.value = false;
  }
}

function selectFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0];
}

async function uploadMaterial() {
  if (!selectedFile.value) {
    documentError.value = "请选择一份文字型 PDF。";
    return;
  }
  if (selectedFile.value.size > MAX_PDF_BYTES) {
    documentError.value = "单个 PDF 文件不得超过 25 MB。";
    return;
  }
  uploading.value = true;
  const targetProject = projectId.value;
  documentError.value = "";
  try {
    await uploadDocument(targetProject, selectedFile.value);
    if (!mounted || targetProject !== projectId.value) return;
    selectedFile.value = undefined;
    if (fileInput.value) fileInput.value.value = "";
    await loadDocuments();
    toast.success("资料已上传并建立索引");
  } catch (error) {
    documentError.value = toAppError(error).message;
  } finally {
    uploading.value = false;
  }
}

async function removeDocument(document: CourseDocument) {
  const targetProject = projectId.value;
  try {
    await deleteDocument(targetProject, document.id);
    pendingDelete.value = undefined;
    if (!mounted || targetProject !== projectId.value) return;
    await loadDocuments();
    toast.success("资料已删除");
  } catch (error) {
    documentError.value = toAppError(error).message;
  }
}

async function ask(text: string) {
  if (
    !text.trim() ||
    !Number.isInteger(projectId.value) ||
    projectId.value <= 0
  )
    return;
  const targetProject = projectId.value;
  const currentQuestion = text.trim();
  errorMessage.value = "";
  try {
    const answer = await tasks.ask(
      targetProject,
      currentQuestion,
      sessionId.value,
    );
    if (!mounted || targetProject !== projectId.value) return;
    sessionId.value = answer.sessionId;
    turns.value.push({ question: currentQuestion, answer });
    question.value = "";
  } catch (error) {
    if (mounted && targetProject === projectId.value)
      errorMessage.value = toAppError(error).message;
  }
}

function submitPrompt(message: PromptInputMessage) {
  return ask(message.text);
}

function useSuggestion(value: string) {
  question.value = value;
  composerKey.value += 1;
}

onMounted(() => {
  mounted = true;
  void Promise.all([loadDocuments(), restoreHistory()]);
});
onUnmounted(() => {
  mounted = false;
});
watch(projectId, () => {
  sessionId.value = undefined;
  turns.value = [];
  selectedFile.value = undefined;
  void Promise.all([loadDocuments(), restoreHistory()]);
});
</script>

<template>
  <section
    class="mx-auto flex h-[calc(100vh-4rem)] max-w-[920px] flex-col px-4 sm:px-7"
  >
    <div class="flex items-center justify-between py-5">
      <h1 class="editorial-title text-2xl font-semibold">知识问答</h1>
      <Button variant="ghost" size="sm" @click="materialsOpen = true">
        <PaperclipIcon />课程资料
        <Badge variant="secondary">{{ documents.length }}</Badge>
      </Button>
    </div>

    <Conversation class="min-h-0" aria-label="课程问答">
      <ConversationContent
        class="mx-auto w-full max-w-[820px] gap-7 px-0 pb-36 pt-6"
      >
        <div
          v-if="!turns.length && !asking"
          class="grid min-h-[48vh] place-items-center text-center"
        >
          <div>
            <h2 class="editorial-title text-balance text-3xl font-semibold">
              今天想弄懂什么？
            </h2>
            <p class="mt-3 text-sm text-muted-foreground">
              直接提问，或从下面选一个开始。
            </p>
            <Suggestions class="mt-6 justify-center">
              <Suggestion
                suggestion="请用一个简单例子解释这门课的核心概念。"
                @click="useSuggestion"
                >解释一个知识点</Suggestion
              >
              <Suggestion
                suggestion="请帮我梳理这门课的重点与区别。"
                @click="useSuggestion"
                >梳理重点与区别</Suggestion
              >
            </Suggestions>
          </div>
        </div>

        <template
          v-for="(turn, index) in turns"
          :key="`${index}-${turn.question}`"
        >
          <Message from="user">
            <MessageContent class="max-w-[36rem] bg-[#efe9e1]">{{
              turn.question
            }}</MessageContent>
          </Message>
          <Message from="assistant" class="max-w-none">
            <MessageContent class="w-full max-w-none text-[15px] leading-7">
              <p class="whitespace-pre-wrap">{{ turn.answer.answer }}</p>
              <p
                v-if="turn.answer.status === 'INSUFFICIENT_EVIDENCE'"
                class="mt-4 border-l-2 border-amber-500/70 pl-3 text-sm text-amber-800"
              >
                当前资料证据不足，回答未使用模型推断。
              </p>
              <div
                v-if="turn.answer.citations.length"
                class="mt-4 flex flex-wrap gap-2"
              >
                <InlineCitation
                  v-for="citation in turn.answer.citations"
                  :key="`${citation.documentName}-${citation.pageNumber}-${citation.excerpt}`"
                >
                  <InlineCitationCard>
                    <InlineCitationCardTrigger
                      :sources="['https://materials.local']"
                      :label="`${citation.documentName} · P${citation.pageNumber}`"
                      class="ml-0 cursor-help font-normal"
                    />
                    <InlineCitationCardBody>
                      <div class="space-y-3 p-4">
                        <InlineCitationSource
                          :title="citation.documentName"
                          :description="`第 ${citation.pageNumber} 页 · 相关度 ${Math.round(citation.score * 100)}%`"
                        />
                        <InlineCitationQuote>{{
                          citation.excerpt
                        }}</InlineCitationQuote>
                      </div>
                    </InlineCitationCardBody>
                  </InlineCitationCard>
                </InlineCitation>
              </div>
            </MessageContent>
          </Message>
        </template>

        <Message v-if="asking" from="assistant">
          <MessageContent
            class="flex-row items-center gap-2 text-muted-foreground"
          >
            <Loader :size="14" /><Shimmer class="text-sm"
              >正在检索课程资料并组织回答…</Shimmer
            >
          </MessageContent>
        </Message>
        <p v-if="errorMessage" class="text-sm text-destructive" role="alert">
          {{ errorMessage }}
        </p>
      </ConversationContent>
      <ConversationScrollButton />
    </Conversation>

    <div
      class="pointer-events-none absolute inset-x-0 bottom-0 z-20 bg-gradient-to-t from-background via-background to-transparent px-4 pb-5 pt-12 md:left-[276px]"
    >
      <PromptInput
        :key="composerKey"
        class="pointer-events-auto mx-auto max-w-[820px]"
        :initial-input="question"
        @submit="submitPrompt"
      >
        <PromptInputBody>
          <PromptInputTextarea
            :disabled="asking"
            maxlength="1000"
            placeholder="输入问题，Shift + Enter 换行"
            class="min-h-[72px] resize-none px-4 pt-4 text-[15px]"
          />
        </PromptInputBody>
        <PromptInputFooter class="px-3 pb-2">
          <PromptInputTools>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              class="h-8 text-xs text-muted-foreground"
              @click="materialsOpen = true"
            >
              <PaperclipIcon />课程资料 {{ documents.length }}
            </Button>
            <span class="hidden text-xs text-muted-foreground sm:inline">{{
              documents.length ? "使用课程资料检索" : "暂无课程资料"
            }}</span>
          </PromptInputTools>
          <PromptInputSubmit
            :disabled="asking"
            :status="asking ? 'submitted' : 'ready'"
            class="rounded-full"
            aria-label="发送问题"
          />
        </PromptInputFooter>
      </PromptInput>
    </div>

    <Sheet v-model:open="materialsOpen">
      <SheetContent class="w-[94vw] bg-card p-0 sm:max-w-lg">
        <SheetHeader class="border-b px-6 py-5 text-left"
          ><SheetTitle>课程资料</SheetTitle
          ><SheetDescription class="sr-only"
            >上传、查看和管理当前课程的 PDF 资料。</SheetDescription
          ></SheetHeader
        >
        <ScrollArea class="h-[calc(100vh-76px)]">
          <div class="space-y-5 p-5">
            <form class="space-y-3" @submit.prevent="uploadMaterial">
              <label
                class="flex min-h-32 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed bg-background px-4 text-center transition-colors hover:bg-accent/40"
              >
                <input
                  ref="fileInput"
                  accept="application/pdf,.pdf"
                  type="file"
                  class="sr-only"
                  @change="selectFile"
                />
                <UploadCloudIcon class="mb-3 size-6 text-muted-foreground" />
                <span class="max-w-full truncate text-sm font-medium">{{
                  selectedFile?.name ?? "拖入或选择 PDF"
                }}</span>
                <small class="mt-1 text-xs text-muted-foreground"
                  >PDF · 最大 25 MB</small
                >
              </label>
              <Button
                type="submit"
                class="w-full"
                :disabled="uploading || !selectedFile"
              >
                <Loader v-if="uploading" :size="14" />{{
                  uploading ? "正在建立索引" : "上传资料"
                }}
              </Button>
            </form>
            <p
              v-if="documentError"
              class="text-sm text-destructive"
              role="alert"
            >
              {{ documentError }}
            </p>
            <div
              v-if="documentsLoading"
              class="flex items-center gap-2 py-8 text-sm text-muted-foreground"
            >
              <Loader />正在读取资料…
            </div>
            <p
              v-else-if="!documents.length"
              class="py-8 text-center text-sm text-muted-foreground"
            >
              尚未添加资料
            </p>
            <Attachments v-else variant="list" aria-label="当前课程资料列表">
              <Attachment
                v-for="document in documents"
                :key="document.id"
                :data="{
                  id: String(document.id),
                  type: 'file',
                  mediaType: 'application/pdf',
                  filename: document.displayName,
                  url: '',
                }"
                class="bg-background"
              >
                <FileTextIcon class="size-5 shrink-0 text-primary" />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium">
                    {{ document.displayName }}
                  </p>
                  <p class="mt-1 text-xs text-muted-foreground">
                    {{ document.pageCount }} 页
                  </p>
                </div>
                <Badge
                  :variant="statusVariant(document.status)"
                  class="shrink-0"
                  >{{ statusLabel(document.status) }}</Badge
                >
                <DropdownMenu>
                  <DropdownMenuTrigger as-child
                    ><Button
                      variant="ghost"
                      size="icon-sm"
                      :aria-label="`资料操作：${document.displayName}`"
                      ><MoreHorizontalIcon /></Button
                  ></DropdownMenuTrigger>
                  <DropdownMenuContent align="end"
                    ><DropdownMenuItem
                      class="text-destructive"
                      @select="pendingDelete = document"
                      ><Trash2Icon />删除资料</DropdownMenuItem
                    ></DropdownMenuContent
                  >
                </DropdownMenu>
              </Attachment>
            </Attachments>
          </div>
        </ScrollArea>
      </SheetContent>
    </Sheet>

    <AlertDialog
      :open="Boolean(pendingDelete)"
      @update:open="(open) => !open && (pendingDelete = undefined)"
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle
            >删除“{{ pendingDelete?.displayName }}”？</AlertDialogTitle
          >
          <AlertDialogDescription
            >这会移除本课程中的文件和检索索引。</AlertDialogDescription
          >
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="pendingDelete = undefined"
            >取消</AlertDialogCancel
          >
          <AlertDialogAction
            class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            @click="pendingDelete && removeDocument(pendingDelete)"
            >删除</AlertDialogAction
          >
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </section>
</template>
