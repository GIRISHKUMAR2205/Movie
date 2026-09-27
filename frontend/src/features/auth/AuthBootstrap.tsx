import { useEffect, useRef, type ReactNode } from "react";

import { useAppDispatch } from "../../app/hooks";
import { authApi } from "../../api/authApi";
import { beginBootstrap, clearAuth } from "./authSlice";

export default function AuthBootstrap({ children }: { children: ReactNode }) {
  const dispatch = useAppDispatch();
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    void (async () => {
      dispatch(beginBootstrap());
      try {
        await dispatch(authApi.endpoints.refresh.initiate()).unwrap();
      } catch {
        dispatch(clearAuth());
      }
    })();
  }, [dispatch]);

  return <>{children}</>;
}
