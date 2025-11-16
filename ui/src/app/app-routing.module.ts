import { NgModule } from '@angular/core';
import { NoPreloading, RouterModule, Routes } from '@angular/router';

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
    path: 'project-selector',
    loadComponent: () =>
      import(
        './pages/project-selector/feature/project-selector.component'
      ).then(x => x.ProjectSelectorComponent),
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
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(
            x => x.DashboardComponent
          ),
      },
      {
        path: 'users',
        loadChildren: () =>
          import('./pages/users/users.routes').then(x => x.UsersRoutes),
      },
      {
        path: 'targets',
        loadChildren: () =>
          import('./pages/targets/targets.routes').then(x => x.TargetsRoutes),
      },
      {
        path: 'agents',
        loadComponent: () =>
          import('./pages/agents/feature/agents.component').then(
            x => x.AgentsComponent
          ),
      },
      {
        path: 'templates',
        loadChildren: () =>
          import('./pages/templates/templates.routes').then(
            x => x.templatesRoutes
          ),
      },
      {
        path: 'alerts',
        loadChildren: () =>
          import('./pages/alerts/alerts.routes').then(x => x.alertsRoutes),
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
