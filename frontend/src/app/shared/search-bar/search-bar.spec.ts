import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ChatService } from '../../chat/chat.service';
import { Conversation } from '../../chat/chat.model';
import { FriendshipService } from '../../friends/friendship.service';
import { SearchBar } from './search-bar';
import { SearchService } from './search.service';
import { SearchResult, SearchSuggestion } from './search.model';

describe('SearchBar', () => {
    let component: SearchBar;
    let fixture: ComponentFixture<SearchBar>;
    let searchServiceSpy: jasmine.SpyObj<SearchService>;
    let friendshipServiceSpy: jasmine.SpyObj<FriendshipService>;
    let chatServiceSpy: jasmine.SpyObj<ChatService>;
    let routerSpy: jasmine.SpyObj<Router>;

    const mockConversation: Conversation = {
        id: 'conversation-1',
        type: 'DIRECT',
        displayName: 'Murat',
        displayAvatar: null,
        members: [],
        lastMessage: null,
        lastMessageAt: null,
    };

    beforeEach(async () => {
        searchServiceSpy = jasmine.createSpyObj<SearchService>('SearchService', [
            'search',
            'getSuggestions',
            'deleteSuggestion',
        ]);

        friendshipServiceSpy = jasmine.createSpyObj<FriendshipService>(
            'FriendshipService',
            ['search']
        );

        chatServiceSpy = jasmine.createSpyObj<ChatService>('ChatService', [
            'openDirect',
        ]);

        routerSpy = jasmine.createSpyObj<Router>('Router', [
            'navigate',
            'navigateByUrl',
        ]);

        searchServiceSpy.search.and.returnValue(of([]));
        searchServiceSpy.getSuggestions.and.returnValue(of([]));
        searchServiceSpy.deleteSuggestion.and.returnValue(of(undefined));
        friendshipServiceSpy.search.and.returnValue(of([]));
        chatServiceSpy.openDirect.and.returnValue(of(mockConversation));

        await TestBed.configureTestingModule({
            imports: [SearchBar],
            providers: [
                { provide: SearchService, useValue: searchServiceSpy },
                { provide: FriendshipService, useValue: friendshipServiceSpy },
                { provide: ChatService, useValue: chatServiceSpy },
                { provide: Router, useValue: routerSpy },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(SearchBar);
        component = fixture.componentInstance;
        fixture.detectChanges();

        searchServiceSpy.search.calls.reset();
        searchServiceSpy.getSuggestions.calls.reset();
        searchServiceSpy.deleteSuggestion.calls.reset();
        friendshipServiceSpy.search.calls.reset();
        chatServiceSpy.openDirect.calls.reset();
        routerSpy.navigate.calls.reset();
        routerSpy.navigateByUrl.calls.reset();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load search suggestions when the panel is opened', () => {
        const suggestions: SearchSuggestion[] = [
            { queryText: 'Paris', count: 3 },
            { queryText: 'Hotel', count: 2 },
        ];

        searchServiceSpy.getSuggestions.and.returnValue(of(suggestions));

        component.openPanel();

        expect(component.isPanelOpen).toBeTrue();
        expect(searchServiceSpy.getSuggestions).toHaveBeenCalled();
        expect(component.suggestions).toEqual(suggestions);
    });

    it('should not search automatically for less than two characters', fakeAsync(() => {
        component.queryControl.setValue('f');

        tick(300);

        expect(searchServiceSpy.search).not.toHaveBeenCalled();
        expect(friendshipServiceSpy.search).not.toHaveBeenCalled();
        expect(component.results).toEqual([]);
        expect(component.hasSearched).toBeFalse();
    }));

    it('should search automatically after two characters', fakeAsync(() => {
        const backendResults: SearchResult[] = [
            {
                id: 'trip-1',
                title: 'France Trip',
                subtitle: 'Paris',
                type: 'Trip',
                route: '/dashboard',
            },
        ];

        searchServiceSpy.search.and.returnValue(of(backendResults));

        component.queryControl.setValue('fr');

        tick(300);

        expect(searchServiceSpy.search).toHaveBeenCalledWith('fr');
        expect(friendshipServiceSpy.search).toHaveBeenCalledWith('fr');
        expect(component.hasSearched).toBeTrue();
        expect(component.results.some((result) => result.title === 'Friends')).toBeTrue();
        expect(component.results.some((result) => result.title === 'France Trip')).toBeTrue();
    }));

    it('should show existing friends in search results', fakeAsync(() => {
        friendshipServiceSpy.search.and.returnValue(
            of([
                {
                    userId: 'friend-1',
                    name: 'Murat',
                    email: 'murat@example.com',
                    profilePicture: null,
                    relationship: 'FRIENDS',
                },
            ])
        );

        component.queryControl.setValue('mu');

        tick(300);

        expect(friendshipServiceSpy.search).toHaveBeenCalledWith('mu');
        expect(component.results).toContain(
            jasmine.objectContaining({
                id: 'friend-1',
                title: 'Murat',
                subtitle: 'murat@example.com',
                type: 'Friend',
                route: '/chats',
            })
        );
    }));

    it('should not show users who are not friends in search results', fakeAsync(() => {
        friendshipServiceSpy.search.and.returnValue(
            of([
                {
                    userId: 'user-1',
                    name: 'Not Friend',
                    email: 'notfriend@example.com',
                    profilePicture: null,
                    relationship: 'NONE',
                },
            ])
        );

        component.queryControl.setValue('no');

        tick(300);

        expect(component.results.some((result) => result.type === 'Friend')).toBeFalse();
    }));

    it('should navigate to a selected feature result', () => {
        component.goToResult({
            id: 'feature-friends',
            title: 'Friends',
            subtitle: 'Open friends page',
            type: 'Feature',
            route: '/friends',
        });

        expect(component.isPanelOpen).toBeFalse();
        expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/friends');
    });

    it('should open direct chat when selecting a friend result', () => {
        chatServiceSpy.openDirect.and.returnValue(of(mockConversation));

        component.goToResult({
            id: 'friend-1',
            title: 'Murat',
            subtitle: 'murat@example.com',
            type: 'Friend',
            route: '/chats',
        });

        expect(chatServiceSpy.openDirect).toHaveBeenCalledWith('friend-1');
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/chats', 'conversation-1']);
    });

    it('should delete a search history item', () => {
        component.suggestions = [
            { queryText: 'Paris', count: 3 },
            { queryText: 'Hotel', count: 2 },
        ];

        component.deleteSuggestion('Paris', new MouseEvent('click'));

        expect(searchServiceSpy.deleteSuggestion).toHaveBeenCalledWith('Paris');
        expect(component.suggestions).toEqual([{ queryText: 'Hotel', count: 2 }]);
    });

    it('should clear search input and results', () => {
        component.queryControl.setValue('Paris', { emitEvent: false });
        component.results = [
            {
                id: 'trip-1',
                title: 'Paris Trip',
                subtitle: 'Paris',
                type: 'Trip',
                route: '/dashboard',
            },
        ];
        component.hasSearched = true;
        component.errorMessage = 'Error';

        component.clearSearch();

        expect(component.queryControl.value).toBe('');
        expect(component.results).toEqual([]);
        expect(component.hasSearched).toBeFalse();
        expect(component.errorMessage).toBe('');
        expect(component.isPanelOpen).toBeFalse();
    });
});
