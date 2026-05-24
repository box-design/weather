import { useCallback, useMemo, useState } from "react";

export function useAuth() {
  const [user, setUser] = useState<null | { name: string; avatar?: string }>(null);

  const logout = useCallback(() => {
    setUser(null);
  }, []);

  return useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading: false,
      error: null,
      logout,
      login: (name: string, avatar?: string) => setUser({ name, avatar }),
    }),
    [user, logout],
  );
}
