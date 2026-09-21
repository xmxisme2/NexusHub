import axios, { type AxiosError, type AxiosRequestConfig } from "axios";

export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
  requestId: string;
};
export type ApiFieldError = {
  field: string;
  code?: string;
  message?: string;
  reason?: string;
};

export class ApiError extends Error {
  readonly status?: number;
  readonly requestId?: string;
  readonly fieldErrors: ApiFieldError[];

  constructor(
    message: string,
    options: {
      status?: number;
      requestId?: string;
      fieldErrors?: ApiFieldError[];
    } = {},
  ) {
    super(message);
    this.name = "ApiError";
    this.status = options.status;
    this.requestId = options.requestId;
    this.fieldErrors = options.fieldErrors ?? [];
  }
}

let csrfToken = "";
let csrfHeaderName = "X-CSRF-TOKEN";
export const http = axios.create({
  baseURL: "/api",
  withCredentials: true,
  timeout: 15_000,
});

http.interceptors.request.use((config) => {
  if (csrfToken && config.method?.toLowerCase() !== "get")
    config.headers.set(csrfHeaderName, csrfToken);
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<{ fieldErrors?: ApiFieldError[] }>>) =>
    Promise.reject(
      new ApiError(
        error.response?.data?.message || "请求未完成，请稍后重试。",
        {
          status: error.response?.status,
          requestId: error.response?.data?.requestId,
          fieldErrors: error.response?.data?.data?.fieldErrors,
        },
      ),
    ),
);

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config);
  return response.data.data;
}

export async function refreshCsrf(): Promise<void> {
  const response =
    await http.get<ApiEnvelope<{ token: string; headerName: string }>>(
      "/auth/csrf",
    );
  csrfToken = response.data.data.token;
  csrfHeaderName = response.data.data.headerName;
}
