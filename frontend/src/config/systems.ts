/** 门户外部系统入口。地址通过构建变量覆盖，默认使用已确认的 OpsDesk 地址。 */
const DEFAULT_OPS_DESK_URL = "https://www.xmxisme.com/opsdesk/";

function externalUrl(value: string | undefined): string {
  const candidate = (value || DEFAULT_OPS_DESK_URL).trim();
  try {
    const url = new URL(candidate);
    if (url.protocol !== "http:" && url.protocol !== "https:")
      return DEFAULT_OPS_DESK_URL;
    return url.toString();
  } catch {
    return DEFAULT_OPS_DESK_URL;
  }
}

export const OPS_DESK_URL = externalUrl(import.meta.env.VITE_OPS_DESK_URL);
