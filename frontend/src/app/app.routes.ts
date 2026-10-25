import { Routes } from '@angular/router';
import { CalendarComponent } from './calendar/calendar';
import { LoginComponent } from './authentication/login/login.component';
import { RegistrationComponent } from './authentication/registration/registration.component';
import { authGuard } from './authentication/auth.guard';
import { BudgetComponent } from './Component/budget/budget.component';
import { Dashboard } from './dashboard/dashboard';
import { VerifyEmailComponent } from './authentication/verify-email/verify-email.component';
import { FriendsComponent } from './friends/friends.component';
import { ChatListComponent } from './chat/chat-list.component';
import { ChatComponent } from './chat/chat.component';
import { LandingPageComponent } from './landing page/landing-page';
import { EmailCostsComponent } from './email-costs/email-costs.component';
import { ProfileComponent } from './profile/profile.component';
import { FilemanagerComponent } from './filemanager/filemanager.component';
import { TripDetailComponent } from './trip-detail/trip-detail.component';
import { TripMap } from './trip-map/trip-map';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'email-costs', component: EmailCostsComponent, canActivate: [authGuard] },
  { path: 'home', component: LandingPageComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegistrationComponent },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
  { path: 'trip/:id', component: TripDetailComponent, canActivate: [authGuard] },
  { path: 'calendar', component: CalendarComponent, canActivate: [authGuard] },
  { path: 'budget', component: BudgetComponent, canActivate: [authGuard] },
  { path: 'friends', component: FriendsComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'chats', component: ChatListComponent, canActivate: [authGuard] },
  { path: 'chats/:id', component: ChatComponent, canActivate: [authGuard] },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'filemanager', component: FilemanagerComponent, canActivate: [authGuard] },
  { path: 'trip-map',    component: TripMap,              canActivate: [authGuard] },
  { path: '**', redirectTo: 'home' },
];
