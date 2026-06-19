from __future__ import annotations

from typing import Any
from typing import Optional

import httpx


class BackendBusinessError(RuntimeError):
    pass


class BackendClient:
    def __init__(self, base_url: str, timeout_seconds: int, token: Optional[str] = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds
        self.token = token

    def _headers(self) -> dict[str, str]:
        headers: dict[str, str] = {}
        if self.token:
            headers["authorization"] = self.token
        return headers

    def _get(self, path: str) -> dict[str, Any]:
        with httpx.Client(timeout=self.timeout_seconds, headers=self._headers()) as client:
            resp = client.get(f"{self.base_url}{path}")
            resp.raise_for_status()
            return self._unwrap_result(resp.json())

    @staticmethod
    def _unwrap_result(payload: dict[str, Any]) -> dict[str, Any]:
        if "success" not in payload:
            return payload
        if payload.get("success") is True:
            data = payload.get("data")
            if isinstance(data, dict):
                return data
            return {"data": data, "total": payload.get("total")}
        error_code = payload.get("errorCode") or "BIZ_ERROR"
        error_msg = payload.get("errorMsg") or "业务接口调用失败"
        raise BackendBusinessError(f"{error_code}: {error_msg}")

    def get_shop_detail(self, shop_id: int) -> dict[str, Any]:
        return self._get(f"/shop/{shop_id}")

    def get_voucher_list(self, shop_id: int) -> dict[str, Any]:
        return self._get(f"/voucher/list/{shop_id}")

    def get_seckill_order_status(self, order_id: int) -> dict[str, Any]:
        return self._get(f"/voucher-order/status/{order_id}")
