import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-authentication-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './authentication-layout.component.html',
  styles: ['.auth__layout {height: 100vh}'],
})
export class AuthenticationLayoutComponent {}
