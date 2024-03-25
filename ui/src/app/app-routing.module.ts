import { NgModule } from '@angular/core';
import { NoPreloading, RouterModule, Routes } from '@angular/router';
import { isAuthenticated } from './shared/guards/is-authenticated.guard';

const routes: Routes = [
  {
    path: 'login',
    pathMatch: 'prefix',
    loadChildren: () =>
      import('./pages/authentication/authentication.routes').then(
        x => x.AuthenticationRoutes
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: '/login',
  },
  {
    path: '',
    loadComponent: () =>
      import('./pages/layout/feature/layout.component').then(
        x => x.LayoutComponent
      ),
    children: [
      {
        path: 'mainpage',
        canMatch: [isAuthenticated],
        loadComponent: () =>
          import('./pages/mainpage/mainpage.component').then(
            x => x.MainpageComponent
          ),
      },
      {
        path: 'dashboard',
        canMatch: [isAuthenticated],
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(
            x => x.DashboardComponent
          ),
      },
      {
        path: 'products',
        canMatch: [isAuthenticated],
        loadChildren: () =>
          import('./pages/products/products.routes').then(x => x.ProductRoutes),
      },
      {
        path: 'categories',
        canMatch: [isAuthenticated],
        loadChildren: () =>
          import('./pages/categories/categories.routes').then(
            x => x.CategoriesRoutes
          ),
      },
      {
        path: 'users',
        canMatch: [isAuthenticated],
        loadChildren: () =>
          import('./pages/users/users.routes').then(x => x.UsersRoutes),
      },
      {
        path: 'page-not-found',
        loadComponent: () =>
          import('./pages/page-not-found/page-not-found.component').then(
            x => x.PageNotFoundComponent
          ),
      },
      {
        path: '**',
        redirectTo: 'page-not-found',
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { preloadingStrategy: NoPreloading })],
  exports: [RouterModule],
})
export class AppRoutingModule {}
