import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { ToastModule } from 'primeng/toast';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LocalStorageService } from './shared/services/local-storage.service';
import { RequestInterceptor } from './shared/interceptors/request.interceptor';
import { ToastService } from './shared/services/toast.service';
import { MessageService } from 'primeng/api';
import { LayoutService } from './shared/services/layout.service';
import { MenuService } from './shared/services/menu.service';
import { MessagesModule } from 'primeng/messages';
import { AccountService } from './shared/services/account.service';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule,
    ToastModule,
    MessagesModule,
  ],
  providers: [
    LocalStorageService,
    ToastService,
    MessageService,
    LayoutService,
    MenuService,
    AccountService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: RequestInterceptor,
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
