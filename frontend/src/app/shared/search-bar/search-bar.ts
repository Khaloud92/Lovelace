import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  OnInit,
  inject,
} from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  debounceTime,
  distinctUntilChanged,
  filter,
  finalize,
  forkJoin,
  map,
  of,
  switchMap,
  tap,
  catchError,
} from 'rxjs';
import { ChatService } from '../../chat/chat.service';
import { FriendshipService } from '../../friends/friendship.service';
import { UserSearchResult } from '../../friends/friendship.model';
import { SearchResult, SearchSuggestion } from './search.model';
import { SearchService } from './search.service';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './search-bar.html',
  styleUrl: './search-bar.css',
})
export class SearchBar implements OnInit {
  private readonly searchService = inject(SearchService);
  private readonly friendshipService = inject(FriendshipService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  queryControl = new FormControl('', { nonNullable: true });

  results: SearchResult[] = [];
  suggestions: SearchSuggestion[] = [];

  isLoading = false;
  isPanelOpen = false;
  hasSearched = false;
  errorMessage = '';

  private readonly featureResults: SearchResult[] = [
    {
      id: 'feature-dashboard',
      title: 'Dashboard',
      subtitle: 'Open dashboard page',
      type: 'Feature',
      route: '/dashboard',
    },
    {
      id: 'feature-calendar',
      title: 'Calendar',
      subtitle: 'Open calendar page',
      type: 'Feature',
      route: '/calendar',
    },
    {
      id: 'feature-budget',
      title: 'Budget Planer',
      subtitle: 'Open budget planer page',
      type: 'Feature',
      route: '/budget',
    },
    {
      id: 'feature-chats',
      title: 'Chats',
      subtitle: 'Open chats page',
      type: 'Feature',
      route: '/chats',
    },
    {
      id: 'feature-friends',
      title: 'Friends',
      subtitle: 'Open friends page',
      type: 'Feature',
      route: '/friends',
    },
    {
      id: 'feature-profile',
      title: 'Profile',
      subtitle: 'Open profile page',
      type: 'Feature',
      route: '/profile',
    },
    {
      id: 'feature-email-costs',
      title: 'EmailCost',
      subtitle: 'Open email costs page',
      type: 'Feature',
      route: '/email-costs',
    },
    {
      id: 'feature-settings',
      title: 'Settings',
      subtitle: 'Open settings page',
      type: 'Feature',
      route: '/settings',
    },
  ];

  ngOnInit(): void {
    this.loadSuggestions();
    this.listenForSearchInput();
  }

  @HostListener('document:click', ['$event'])
  closePanelOnOutsideClick(event: MouseEvent): void {
    const clickedInsideSearchBar = this.elementRef.nativeElement.contains(
        event.target as Node
    );

    if (!clickedInsideSearchBar) {
      this.closePanel();
    }
  }

  openPanel(): void {
    this.isPanelOpen = true;
    this.loadSuggestions();
  }

  closePanel(): void {
    this.isPanelOpen = false;
  }

  searchFromInput(): void {
    const query = this.queryControl.value.trim();

    if (!query) {
      this.results = [];
      this.hasSearched = false;
      this.errorMessage = '';
      this.closePanel();
      return;
    }

    this.performSearch(query);
  }

  useSuggestion(queryText: string): void {
    this.queryControl.setValue(queryText, { emitEvent: false });
    this.performSearch(queryText);
  }

  deleteSuggestion(queryText: string, event: MouseEvent): void {
    event.stopPropagation();

    this.searchService
        .deleteSuggestion(queryText)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.suggestions = this.suggestions.filter(
                (suggestion) => suggestion.queryText !== queryText
            );
          },
          error: () => {
            this.errorMessage = 'Could not delete search history item.';
          },
        });
  }

  goToResult(result: SearchResult): void {
    this.closePanel();

    if (result.type === 'Friend') {
      this.openFriendChat(result.id);
      return;
    }

    this.router.navigateByUrl(result.route);
  }

  clearSearch(): void {
    this.queryControl.setValue('', { emitEvent: false });
    this.results = [];
    this.hasSearched = false;
    this.errorMessage = '';
    this.isLoading = false;
    this.closePanel();
  }

  private listenForSearchInput(): void {
    this.queryControl.valueChanges
        .pipe(
            takeUntilDestroyed(this.destroyRef),
            debounceTime(300),
            map((value) => value.trim()),
            distinctUntilChanged(),
            tap((query) => {
              this.errorMessage = '';

              if (query.length < 2) {
                this.results = [];
                this.hasSearched = false;
                this.isLoading = false;
                return;
              }

              this.isPanelOpen = true;
            }),
            filter((query) => query.length >= 2),
            tap(() => {
              this.isLoading = true;
              this.hasSearched = true;
            }),
            switchMap((query) =>
                this.searchAll(query).pipe(
                    finalize(() => {
                      this.isLoading = false;
                    })
                )
            )
        )
        .subscribe((results) => {
          this.results = results;
          this.loadSuggestions();
        });
  }

  private performSearch(query: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.hasSearched = true;
    this.isPanelOpen = true;

    this.searchAll(query)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (results) => {
            this.results = results;
            this.isLoading = false;
            this.loadSuggestions();
          },
          error: () => {
            this.results = [];
            this.isLoading = false;
            this.errorMessage = 'Search failed. Please try again.';
          },
        });
  }

  private searchAll(query: string) {
    const featureResults = this.getFeatureResults(query);

    return forkJoin({
      backendResults: this.searchService.search(query).pipe(
          catchError(() => of([] as SearchResult[]))
      ),
      friendResults: this.friendshipService.search(query).pipe(
          map((friends) => this.mapFriendResults(friends)),
          catchError(() => of([] as SearchResult[]))
      ),
    }).pipe(
        map(({ backendResults, friendResults }) =>
            this.mergeResults(featureResults, friendResults, backendResults)
        )
    );
  }

  private mapFriendResults(friends: UserSearchResult[]): SearchResult[] {
    return friends
        .filter((friend) => friend.relationship === 'FRIENDS')
        .map((friend) => ({
          id: friend.userId,
          title: friend.name,
          subtitle: friend.email,
          type: 'Friend',
          route: '/chats',
        }));
  }

  private openFriendChat(friendId: string): void {
    this.chatService
        .openDirect(friendId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (conversation) => {
            this.router.navigate(['/chats', conversation.id]);
          },
          error: () => {
            this.errorMessage = 'Could not open chat with this friend.';
            this.isPanelOpen = true;
          },
        });
  }

  private getFeatureResults(query: string): SearchResult[] {
    const normalizedQuery = query.toLowerCase();

    return this.featureResults.filter((feature) => {
      const searchableText = [
        feature.title,
        feature.subtitle,
        feature.type,
        feature.route,
      ]
          .join(' ')
          .toLowerCase();

      return searchableText.includes(normalizedQuery);
    });
  }

  private mergeResults(...resultGroups: SearchResult[][]): SearchResult[] {
    const uniqueResults = new Map<string, SearchResult>();

    resultGroups.flat().forEach((result) => {
      uniqueResults.set(`${result.type}-${result.id}`, result);
    });

    return [...uniqueResults.values()];
  }

  private loadSuggestions(): void {
    this.searchService
        .getSuggestions()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (suggestions) => {
            this.suggestions = suggestions;
          },
          error: () => {
            this.suggestions = [];
          },
        });
  }
}
