import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = (route, state) => {
  const user = localStorage.getItem('merchant');
  if (user) {
    return true;
  }
  const router = new Router();
  router.navigate(['/login']);
  return false;
};
