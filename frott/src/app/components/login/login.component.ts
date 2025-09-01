import { Component } from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {CommonModule} from '@angular/common';

interface LoginPayload {
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  standalone: true,
  imports: [CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  message: string = '';
  messageType: 'success' | 'error' | '' = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      rememberMe: [false]
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;

      const payload: LoginPayload = {
        email: this.loginForm.value.email,
        password: this.loginForm.value.password
      };
      this.authService.login(payload).subscribe({
        next: (res: any) => {
          this.isLoading = false;
          this.message = 'Connexion réussie !';
          this.messageType = 'success';

          // Stocker le token et l'utilisateur
          localStorage.setItem('user', JSON.stringify(res.user));
          localStorage.setItem('token', res.token);
          localStorage.setItem('roles', JSON.stringify(res.authorities || res.user.roles || []));

          const roles = res.authorities || res.user.roles || [];

          // Redirection selon le rôle
          if (this.authService.isAdmin()) {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/home']);
          }
        },
        error: (err) => {
          this.isLoading = false;
          this.message = 'Erreur : ' + (err.error?.message || 'Échec de la connexion');
          this.messageType = 'error';
          console.error('Login error:', err);
        }
      });
    }}}
